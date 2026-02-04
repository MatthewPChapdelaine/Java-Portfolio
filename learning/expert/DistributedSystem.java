import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Raft Consensus Algorithm Implementation
 * Distributed consensus with leader election, log replication, and failure handling
 * Supports multi-node simulation with network partitions and node failures
 */
public class DistributedSystem {
    
    // ============ LOG ENTRIES ============
    static class LogEntry {
        final int term;
        final String command;
        final int index;
        
        LogEntry(int term, String command, int index) {
            this.term = term;
            this.command = command;
            this.index = index;
        }
        
        @Override
        public String toString() {
            return String.format("[%d:T%d:%s]", index, term, command);
        }
    }
    
    // ============ RPC MESSAGES ============
    static class RequestVoteRequest {
        final int term;
        final int candidateId;
        final int lastLogIndex;
        final int lastLogTerm;
        
        RequestVoteRequest(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
            this.term = term;
            this.candidateId = candidateId;
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
        }
    }
    
    static class RequestVoteResponse {
        final int term;
        final boolean voteGranted;
        
        RequestVoteResponse(int term, boolean voteGranted) {
            this.term = term;
            this.voteGranted = voteGranted;
        }
    }
    
    static class AppendEntriesRequest {
        final int term;
        final int leaderId;
        final int prevLogIndex;
        final int prevLogTerm;
        final List<LogEntry> entries;
        final int leaderCommit;
        
        AppendEntriesRequest(int term, int leaderId, int prevLogIndex, int prevLogTerm,
                           List<LogEntry> entries, int leaderCommit) {
            this.term = term;
            this.leaderId = leaderId;
            this.prevLogIndex = prevLogIndex;
            this.prevLogTerm = prevLogTerm;
            this.entries = entries;
            this.leaderCommit = leaderCommit;
        }
    }
    
    static class AppendEntriesResponse {
        final int term;
        final boolean success;
        final int matchIndex;
        
        AppendEntriesResponse(int term, boolean success, int matchIndex) {
            this.term = term;
            this.success = success;
            this.matchIndex = matchIndex;
        }
    }
    
    // ============ NODE STATE ============
    enum NodeState {
        FOLLOWER, CANDIDATE, LEADER
    }
    
    static class RaftNode {
        private final int nodeId;
        private final List<RaftNode> cluster;
        private final Random random = new Random();
        private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        
        // Persistent state
        private int currentTerm = 0;
        private Integer votedFor = null;
        private final List<LogEntry> log = new ArrayList<>();
        
        // Volatile state
        private int commitIndex = 0;
        private int lastApplied = 0;
        private NodeState state = NodeState.FOLLOWER;
        
        // Leader state
        private Map<Integer, Integer> nextIndex = new HashMap<>();
        private Map<Integer, Integer> matchIndex = new HashMap<>();
        
        // Timing
        private volatile long lastHeartbeat = System.currentTimeMillis();
        private ScheduledFuture<?> electionTimer;
        private ScheduledFuture<?> heartbeatTimer;
        
        // State machine
        private final List<String> stateMachine = new ArrayList<>();
        
        // Failure simulation
        private volatile boolean crashed = false;
        private volatile Set<Integer> partitionedFrom = ConcurrentHashMap.newKeySet();
        
        RaftNode(int nodeId, List<RaftNode> cluster) {
            this.nodeId = nodeId;
            this.cluster = cluster;
        }
        
        void start() {
            resetElectionTimer();
            System.out.printf("[Node %d] Started as FOLLOWER\n", nodeId);
        }
        
        void stop() {
            if (electionTimer != null) electionTimer.cancel(false);
            if (heartbeatTimer != null) heartbeatTimer.cancel(false);
            executor.shutdown();
        }
        
        // ============ LEADER ELECTION ============
        private void resetElectionTimer() {
            if (electionTimer != null) {
                electionTimer.cancel(false);
            }
            
            int timeout = 150 + random.nextInt(150); // 150-300ms
            electionTimer = executor.schedule(this::startElection, timeout, TimeUnit.MILLISECONDS);
        }
        
        private void startElection() {
            if (crashed) return;
            
            synchronized (this) {
                state = NodeState.CANDIDATE;
                currentTerm++;
                votedFor = nodeId;
                lastHeartbeat = System.currentTimeMillis();
                
                System.out.printf("[Node %d] Starting election for term %d\n", nodeId, currentTerm);
                
                int lastLogIndex = log.size() - 1;
                int lastLogTerm = lastLogIndex >= 0 ? log.get(lastLogIndex).term : 0;
                
                RequestVoteRequest request = new RequestVoteRequest(
                    currentTerm, nodeId, lastLogIndex, lastLogTerm);
                
                AtomicInteger votes = new AtomicInteger(1); // Vote for self
                int quorum = (cluster.size() / 2) + 1;
                
                for (RaftNode node : cluster) {
                    if (node.nodeId == this.nodeId) continue;
                    if (partitionedFrom.contains(node.nodeId)) continue;
                    
                    executor.submit(() -> {
                        RequestVoteResponse response = node.requestVote(request);
                        if (response != null && response.voteGranted) {
                            int totalVotes = votes.incrementAndGet();
                            if (totalVotes >= quorum && state == NodeState.CANDIDATE) {
                                becomeLeader();
                            }
                        } else if (response != null && response.term > currentTerm) {
                            stepDown(response.term);
                        }
                    });
                }
                
                resetElectionTimer();
            }
        }
        
        private void becomeLeader() {
            synchronized (this) {
                if (state != NodeState.CANDIDATE) return;
                
                state = NodeState.LEADER;
                System.out.printf("[Node %d] Became LEADER for term %d\n", nodeId, currentTerm);
                
                // Initialize leader state
                nextIndex.clear();
                matchIndex.clear();
                for (RaftNode node : cluster) {
                    if (node.nodeId != this.nodeId) {
                        nextIndex.put(node.nodeId, log.size());
                        matchIndex.put(node.nodeId, -1);
                    }
                }
                
                // Start sending heartbeats
                if (heartbeatTimer != null) {
                    heartbeatTimer.cancel(false);
                }
                heartbeatTimer = executor.scheduleAtFixedRate(
                    this::sendHeartbeats, 0, 50, TimeUnit.MILLISECONDS);
                
                if (electionTimer != null) {
                    electionTimer.cancel(false);
                }
            }
        }
        
        private void stepDown(int newTerm) {
            synchronized (this) {
                if (newTerm > currentTerm) {
                    currentTerm = newTerm;
                    votedFor = null;
                    state = NodeState.FOLLOWER;
                    
                    if (heartbeatTimer != null) {
                        heartbeatTimer.cancel(false);
                        heartbeatTimer = null;
                    }
                    
                    resetElectionTimer();
                    System.out.printf("[Node %d] Stepped down to FOLLOWER (term %d)\n", nodeId, currentTerm);
                }
            }
        }
        
        // ============ RPC HANDLERS ============
        synchronized RequestVoteResponse requestVote(RequestVoteRequest request) {
            if (crashed) return null;
            if (partitionedFrom.contains(request.candidateId)) return null;
            
            if (request.term > currentTerm) {
                currentTerm = request.term;
                votedFor = null;
                state = NodeState.FOLLOWER;
            }
            
            boolean voteGranted = false;
            if (request.term == currentTerm) {
                if (votedFor == null || votedFor == request.candidateId) {
                    // Check log is at least as up-to-date
                    int lastLogIndex = log.size() - 1;
                    int lastLogTerm = lastLogIndex >= 0 ? log.get(lastLogIndex).term : 0;
                    
                    boolean logUpToDate = request.lastLogTerm > lastLogTerm ||
                        (request.lastLogTerm == lastLogTerm && request.lastLogIndex >= lastLogIndex);
                    
                    if (logUpToDate) {
                        votedFor = request.candidateId;
                        voteGranted = true;
                        lastHeartbeat = System.currentTimeMillis();
                        resetElectionTimer();
                    }
                }
            }
            
            return new RequestVoteResponse(currentTerm, voteGranted);
        }
        
        synchronized AppendEntriesResponse appendEntries(AppendEntriesRequest request) {
            if (crashed) return null;
            if (partitionedFrom.contains(request.leaderId)) return null;
            
            lastHeartbeat = System.currentTimeMillis();
            
            if (request.term > currentTerm) {
                stepDown(request.term);
            }
            
            if (request.term < currentTerm) {
                return new AppendEntriesResponse(currentTerm, false, -1);
            }
            
            // Valid leader for current term
            if (state != NodeState.FOLLOWER) {
                stepDown(request.term);
            }
            resetElectionTimer();
            
            // Check log consistency
            if (request.prevLogIndex >= 0) {
                if (request.prevLogIndex >= log.size() ||
                    log.get(request.prevLogIndex).term != request.prevLogTerm) {
                    return new AppendEntriesResponse(currentTerm, false, -1);
                }
            }
            
            // Append new entries
            int logIndex = request.prevLogIndex + 1;
            for (LogEntry entry : request.entries) {
                if (logIndex < log.size()) {
                    if (log.get(logIndex).term != entry.term) {
                        // Remove conflicting entries
                        log.subList(logIndex, log.size()).clear();
                        log.add(entry);
                    }
                } else {
                    log.add(entry);
                }
                logIndex++;
            }
            
            // Update commit index
            if (request.leaderCommit > commitIndex) {
                commitIndex = Math.min(request.leaderCommit, log.size() - 1);
                applyCommittedEntries();
            }
            
            return new AppendEntriesResponse(currentTerm, true, log.size() - 1);
        }
        
        // ============ LEADER OPERATIONS ============
        private void sendHeartbeats() {
            if (crashed || state != NodeState.LEADER) return;
            
            for (RaftNode node : cluster) {
                if (node.nodeId == this.nodeId) continue;
                if (partitionedFrom.contains(node.nodeId)) continue;
                
                executor.submit(() -> sendAppendEntries(node));
            }
        }
        
        private void sendAppendEntries(RaftNode node) {
            synchronized (this) {
                if (state != NodeState.LEADER) return;
                
                int next = nextIndex.getOrDefault(node.nodeId, log.size());
                int prevLogIndex = next - 1;
                int prevLogTerm = prevLogIndex >= 0 ? log.get(prevLogIndex).term : 0;
                
                List<LogEntry> entries = new ArrayList<>();
                if (next < log.size()) {
                    entries = new ArrayList<>(log.subList(next, log.size()));
                }
                
                AppendEntriesRequest request = new AppendEntriesRequest(
                    currentTerm, nodeId, prevLogIndex, prevLogTerm, entries, commitIndex);
                
                AppendEntriesResponse response = node.appendEntries(request);
                
                if (response != null) {
                    if (response.term > currentTerm) {
                        stepDown(response.term);
                    } else if (state == NodeState.LEADER && response.term == currentTerm) {
                        if (response.success) {
                            nextIndex.put(node.nodeId, response.matchIndex + 1);
                            matchIndex.put(node.nodeId, response.matchIndex);
                            updateCommitIndex();
                        } else {
                            nextIndex.put(node.nodeId, Math.max(0, next - 1));
                        }
                    }
                }
            }
        }
        
        private void updateCommitIndex() {
            for (int n = commitIndex + 1; n < log.size(); n++) {
                if (log.get(n).term != currentTerm) continue;
                
                int replicationCount = 1; // Leader
                for (int match : matchIndex.values()) {
                    if (match >= n) replicationCount++;
                }
                
                if (replicationCount > cluster.size() / 2) {
                    commitIndex = n;
                    applyCommittedEntries();
                }
            }
        }
        
        private void applyCommittedEntries() {
            while (lastApplied < commitIndex) {
                lastApplied++;
                LogEntry entry = log.get(lastApplied);
                stateMachine.add(entry.command);
                System.out.printf("[Node %d] Applied: %s (index=%d, term=%d)\n",
                    nodeId, entry.command, lastApplied, entry.term);
            }
        }
        
        // ============ CLIENT OPERATIONS ============
        synchronized boolean submitCommand(String command) {
            if (state != NodeState.LEADER || crashed) {
                return false;
            }
            
            LogEntry entry = new LogEntry(currentTerm, command, log.size());
            log.add(entry);
            System.out.printf("[Node %d] Received command: %s\n", nodeId, command);
            
            // Immediately replicate
            sendHeartbeats();
            return true;
        }
        
        // ============ FAILURE SIMULATION ============
        void crash() {
            crashed = true;
            System.out.printf("[Node %d] CRASHED\n", nodeId);
        }
        
        void recover() {
            crashed = false;
            state = NodeState.FOLLOWER;
            resetElectionTimer();
            System.out.printf("[Node %d] RECOVERED\n", nodeId);
        }
        
        void partitionFrom(int... nodeIds) {
            for (int id : nodeIds) {
                partitionedFrom.add(id);
            }
            System.out.printf("[Node %d] Partitioned from: %s\n", nodeId, 
                Arrays.toString(nodeIds));
        }
        
        void healPartition() {
            partitionedFrom.clear();
            System.out.printf("[Node %d] Partition healed\n", nodeId);
        }
        
        // ============ STATUS ============
        synchronized String getStatus() {
            return String.format("Node %d: %s (term=%d, log=%d, commit=%d, applied=%d)%s",
                nodeId, state, currentTerm, log.size(), commitIndex, lastApplied,
                crashed ? " [CRASHED]" : "");
        }
        
        synchronized List<String> getStateMachine() {
            return new ArrayList<>(stateMachine);
        }
        
        synchronized boolean isLeader() {
            return state == NodeState.LEADER && !crashed;
        }
        
        int getNodeId() {
            return nodeId;
        }
    }
    
    // ============ CLUSTER MANAGEMENT ============
    static class RaftCluster {
        private final List<RaftNode> nodes;
        
        RaftCluster(int nodeCount) {
            nodes = new ArrayList<>();
            for (int i = 0; i < nodeCount; i++) {
                nodes.add(new RaftNode(i, nodes));
            }
        }
        
        void start() {
            for (RaftNode node : nodes) {
                node.start();
            }
        }
        
        void stop() {
            for (RaftNode node : nodes) {
                node.stop();
            }
        }
        
        RaftNode getLeader() {
            for (RaftNode node : nodes) {
                if (node.isLeader()) {
                    return node;
                }
            }
            return null;
        }
        
        boolean submitCommand(String command) {
            RaftNode leader = getLeader();
            if (leader != null) {
                return leader.submitCommand(command);
            }
            return false;
        }
        
        void printStatus() {
            System.out.println("\n=== Cluster Status ===");
            for (RaftNode node : nodes) {
                System.out.println(node.getStatus());
            }
            System.out.println();
        }
        
        List<RaftNode> getNodes() {
            return nodes;
        }
        
        boolean checkConsensus() {
            List<List<String>> stateMachines = nodes.stream()
                .map(RaftNode::getStateMachine)
                .collect(Collectors.toList());
            
            if (stateMachines.isEmpty()) return true;
            
            List<String> reference = stateMachines.get(0);
            for (List<String> sm : stateMachines) {
                if (!sm.equals(reference)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    // ============ DEMO ============
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Distributed System with Raft Consensus ===\n");
        
        // Create a 5-node cluster
        RaftCluster cluster = new RaftCluster(5);
        cluster.start();
        
        System.out.println("Cluster started. Waiting for leader election...\n");
        Thread.sleep(500);
        
        cluster.printStatus();
        
        // Submit commands
        System.out.println("=== Submitting Commands ===");
        String[] commands = {"SET x=10", "SET y=20", "ADD x y", "PRINT result"};
        
        for (String cmd : commands) {
            RaftNode leader = cluster.getLeader();
            if (leader != null) {
                System.out.printf("Submitting '%s' to leader (Node %d)\n", cmd, leader.getNodeId());
                cluster.submitCommand(cmd);
                Thread.sleep(100);
            } else {
                System.out.println("No leader available!");
            }
        }
        
        Thread.sleep(500);
        cluster.printStatus();
        
        // Simulate leader failure
        System.out.println("\n=== Simulating Leader Failure ===");
        RaftNode leader = cluster.getLeader();
        if (leader != null) {
            System.out.printf("Crashing leader (Node %d)\n", leader.getNodeId());
            leader.crash();
        }
        
        Thread.sleep(500);
        System.out.println("Waiting for new leader election...");
        Thread.sleep(500);
        
        cluster.printStatus();
        
        // Submit more commands
        System.out.println("=== Submitting More Commands ===");
        cluster.submitCommand("SET z=30");
        Thread.sleep(100);
        cluster.submitCommand("MULTIPLY x z");
        Thread.sleep(500);
        
        cluster.printStatus();
        
        // Recover crashed node
        System.out.println("\n=== Recovering Crashed Node ===");
        if (leader != null) {
            leader.recover();
        }
        Thread.sleep(1000);
        
        cluster.printStatus();
        
        // Network partition
        System.out.println("=== Simulating Network Partition ===");
        List<RaftNode> nodes = cluster.getNodes();
        nodes.get(0).partitionFrom(3, 4);
        nodes.get(1).partitionFrom(3, 4);
        nodes.get(2).partitionFrom(3, 4);
        nodes.get(3).partitionFrom(0, 1, 2);
        nodes.get(4).partitionFrom(0, 1, 2);
        
        Thread.sleep(1000);
        cluster.printStatus();
        
        System.out.println("Healing partition...");
        for (RaftNode node : nodes) {
            node.healPartition();
        }
        
        Thread.sleep(1000);
        cluster.printStatus();
        
        // Final state
        System.out.println("=== Final State Machines ===");
        for (int i = 0; i < nodes.size(); i++) {
            List<String> sm = nodes.get(i).getStateMachine();
            System.out.printf("Node %d: %s\n", i, sm);
        }
        
        boolean consensus = cluster.checkConsensus();
        System.out.printf("\nConsensus achieved: %s\n", consensus);
        
        cluster.stop();
        System.out.println("\nCluster stopped.");
    }
}
