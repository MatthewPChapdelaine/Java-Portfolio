import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Machine Learning Library from Scratch
 * Implements: Neural Networks with backpropagation, Linear Regression, Logistic Regression
 * Includes activation functions, loss functions, optimizers, and training utilities
 */
public class MachineLearning {
    
    // ============ MATRIX OPERATIONS ============
    static class Matrix {
        final double[][] data;
        final int rows;
        final int cols;
        
        Matrix(int rows, int cols) {
            this.rows = rows;
            this.cols = cols;
            this.data = new double[rows][cols];
        }
        
        Matrix(double[][] data) {
            this.rows = data.length;
            this.cols = data[0].length;
            this.data = data;
        }
        
        static Matrix fromArray(double[] arr) {
            Matrix m = new Matrix(arr.length, 1);
            for (int i = 0; i < arr.length; i++) {
                m.data[i][0] = arr[i];
            }
            return m;
        }
        
        double[] toArray() {
            double[] arr = new double[rows * cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    arr[i * cols + j] = data[i][j];
                }
            }
            return arr;
        }
        
        void randomize() {
            Random rand = new Random();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    data[i][j] = rand.nextGaussian() * 0.1;
                }
            }
        }
        
        Matrix dot(Matrix other) {
            if (cols != other.rows) {
                throw new IllegalArgumentException("Matrix dimensions don't match");
            }
            Matrix result = new Matrix(rows, other.cols);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < other.cols; j++) {
                    double sum = 0;
                    for (int k = 0; k < cols; k++) {
                        sum += data[i][k] * other.data[k][j];
                    }
                    result.data[i][j] = sum;
                }
            }
            return result;
        }
        
        Matrix transpose() {
            Matrix result = new Matrix(cols, rows);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result.data[j][i] = data[i][j];
                }
            }
            return result;
        }
        
        Matrix add(Matrix other) {
            Matrix result = new Matrix(rows, cols);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result.data[i][j] = data[i][j] + other.data[i][j];
                }
            }
            return result;
        }
        
        Matrix subtract(Matrix other) {
            Matrix result = new Matrix(rows, cols);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result.data[i][j] = data[i][j] - other.data[i][j];
                }
            }
            return result;
        }
        
        Matrix multiply(Matrix other) {
            Matrix result = new Matrix(rows, cols);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result.data[i][j] = data[i][j] * other.data[i][j];
                }
            }
            return result;
        }
        
        Matrix scale(double scalar) {
            Matrix result = new Matrix(rows, cols);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result.data[i][j] = data[i][j] * scalar;
                }
            }
            return result;
        }
        
        Matrix map(Function<Double, Double> func) {
            Matrix result = new Matrix(rows, cols);
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    result.data[i][j] = func.apply(data[i][j]);
                }
            }
            return result;
        }
        
        Matrix copy() {
            Matrix result = new Matrix(rows, cols);
            for (int i = 0; i < rows; i++) {
                System.arraycopy(data[i], 0, result.data[i], 0, cols);
            }
            return result;
        }
    }
    
    // ============ ACTIVATION FUNCTIONS ============
    interface ActivationFunction {
        double activate(double x);
        double derivative(double x);
    }
    
    static class Sigmoid implements ActivationFunction {
        @Override
        public double activate(double x) {
            return 1.0 / (1.0 + Math.exp(-x));
        }
        
        @Override
        public double derivative(double x) {
            double s = activate(x);
            return s * (1 - s);
        }
    }
    
    static class Tanh implements ActivationFunction {
        @Override
        public double activate(double x) {
            return Math.tanh(x);
        }
        
        @Override
        public double derivative(double x) {
            double t = Math.tanh(x);
            return 1 - t * t;
        }
    }
    
    static class ReLU implements ActivationFunction {
        @Override
        public double activate(double x) {
            return Math.max(0, x);
        }
        
        @Override
        public double derivative(double x) {
            return x > 0 ? 1 : 0;
        }
    }
    
    static class Softmax {
        static double[] apply(double[] x) {
            double max = Arrays.stream(x).max().orElse(0);
            double[] exp = Arrays.stream(x).map(v -> Math.exp(v - max)).toArray();
            double sum = Arrays.stream(exp).sum();
            return Arrays.stream(exp).map(v -> v / sum).toArray();
        }
    }
    
    // ============ LOSS FUNCTIONS ============
    static class MSELoss {
        static double compute(Matrix predicted, Matrix actual) {
            double sum = 0;
            for (int i = 0; i < predicted.rows; i++) {
                for (int j = 0; j < predicted.cols; j++) {
                    double diff = predicted.data[i][j] - actual.data[i][j];
                    sum += diff * diff;
                }
            }
            return sum / (predicted.rows * predicted.cols);
        }
        
        static Matrix gradient(Matrix predicted, Matrix actual) {
            Matrix result = new Matrix(predicted.rows, predicted.cols);
            for (int i = 0; i < predicted.rows; i++) {
                for (int j = 0; j < predicted.cols; j++) {
                    result.data[i][j] = 2 * (predicted.data[i][j] - actual.data[i][j]) / 
                        (predicted.rows * predicted.cols);
                }
            }
            return result;
        }
    }
    
    static class CrossEntropyLoss {
        static double compute(Matrix predicted, Matrix actual) {
            double sum = 0;
            for (int i = 0; i < predicted.rows; i++) {
                for (int j = 0; j < predicted.cols; j++) {
                    double p = Math.max(predicted.data[i][j], 1e-15);
                    sum -= actual.data[i][j] * Math.log(p);
                }
            }
            return sum / predicted.rows;
        }
    }
    
    // ============ NEURAL NETWORK ============
    static class NeuralNetwork {
        private final List<Layer> layers = new ArrayList<>();
        private double learningRate;
        
        static class Layer {
            Matrix weights;
            Matrix biases;
            Matrix lastInput;
            Matrix lastOutput;
            Matrix lastZ;
            ActivationFunction activation;
            
            Layer(int inputSize, int outputSize, ActivationFunction activation) {
                this.weights = new Matrix(outputSize, inputSize);
                this.biases = new Matrix(outputSize, 1);
                this.activation = activation;
                
                weights.randomize();
                biases.randomize();
            }
            
            Matrix forward(Matrix input) {
                lastInput = input;
                lastZ = weights.dot(input).add(biases);
                lastOutput = lastZ.map(activation::activate);
                return lastOutput;
            }
            
            Matrix backward(Matrix outputGradient, double learningRate) {
                Matrix activationGradient = lastZ.map(activation::derivative);
                Matrix delta = outputGradient.multiply(activationGradient);
                
                Matrix weightsGradient = delta.dot(lastInput.transpose());
                Matrix inputGradient = weights.transpose().dot(delta);
                
                weights = weights.subtract(weightsGradient.scale(learningRate));
                biases = biases.subtract(delta.scale(learningRate));
                
                return inputGradient;
            }
        }
        
        NeuralNetwork(double learningRate) {
            this.learningRate = learningRate;
        }
        
        void addLayer(int inputSize, int outputSize, ActivationFunction activation) {
            layers.add(new Layer(inputSize, outputSize, activation));
        }
        
        Matrix predict(Matrix input) {
            Matrix output = input;
            for (Layer layer : layers) {
                output = layer.forward(output);
            }
            return output;
        }
        
        void train(Matrix input, Matrix target) {
            Matrix output = predict(input);
            Matrix gradient = MSELoss.gradient(output, target);
            
            for (int i = layers.size() - 1; i >= 0; i--) {
                gradient = layers.get(i).backward(gradient, learningRate);
            }
        }
        
        void trainBatch(List<Matrix> inputs, List<Matrix> targets, int epochs) {
            for (int epoch = 0; epoch < epochs; epoch++) {
                double totalLoss = 0;
                for (int i = 0; i < inputs.size(); i++) {
                    Matrix output = predict(inputs.get(i));
                    totalLoss += MSELoss.compute(output, targets.get(i));
                    train(inputs.get(i), targets.get(i));
                }
                
                if (epoch % 100 == 0) {
                    System.out.printf("Epoch %d: Loss = %.6f\n", epoch, totalLoss / inputs.size());
                }
            }
        }
    }
    
    // ============ LINEAR REGRESSION ============
    static class LinearRegression {
        private Matrix weights;
        private double bias;
        private final double learningRate;
        
        LinearRegression(int features, double learningRate) {
            this.learningRate = learningRate;
            this.weights = new Matrix(features, 1);
            this.weights.randomize();
            this.bias = 0;
        }
        
        double predict(double[] x) {
            double sum = bias;
            for (int i = 0; i < x.length; i++) {
                sum += x[i] * weights.data[i][0];
            }
            return sum;
        }
        
        void train(double[][] X, double[] y, int epochs) {
            int n = X.length;
            
            for (int epoch = 0; epoch < epochs; epoch++) {
                double totalLoss = 0;
                
                for (int i = 0; i < n; i++) {
                    double prediction = predict(X[i]);
                    double error = prediction - y[i];
                    totalLoss += error * error;
                    
                    // Update weights and bias
                    for (int j = 0; j < X[i].length; j++) {
                        weights.data[j][0] -= learningRate * error * X[i][j] / n;
                    }
                    bias -= learningRate * error / n;
                }
                
                if (epoch % 100 == 0) {
                    System.out.printf("Epoch %d: MSE = %.6f\n", epoch, totalLoss / n);
                }
            }
        }
        
        double[] getWeights() {
            return weights.toArray();
        }
        
        double getBias() {
            return bias;
        }
    }
    
    // ============ LOGISTIC REGRESSION ============
    static class LogisticRegression {
        private Matrix weights;
        private double bias;
        private final double learningRate;
        private final Sigmoid sigmoid = new Sigmoid();
        
        LogisticRegression(int features, double learningRate) {
            this.learningRate = learningRate;
            this.weights = new Matrix(features, 1);
            this.weights.randomize();
            this.bias = 0;
        }
        
        double predict(double[] x) {
            double z = bias;
            for (int i = 0; i < x.length; i++) {
                z += x[i] * weights.data[i][0];
            }
            return sigmoid.activate(z);
        }
        
        int classify(double[] x) {
            return predict(x) >= 0.5 ? 1 : 0;
        }
        
        void train(double[][] X, int[] y, int epochs) {
            int n = X.length;
            
            for (int epoch = 0; epoch < epochs; epoch++) {
                double totalLoss = 0;
                int correct = 0;
                
                for (int i = 0; i < n; i++) {
                    double prediction = predict(X[i]);
                    double error = prediction - y[i];
                    
                    // Cross-entropy loss
                    totalLoss -= y[i] * Math.log(prediction + 1e-15) + 
                                (1 - y[i]) * Math.log(1 - prediction + 1e-15);
                    
                    if (classify(X[i]) == y[i]) correct++;
                    
                    // Update weights and bias
                    for (int j = 0; j < X[i].length; j++) {
                        weights.data[j][0] -= learningRate * error * X[i][j];
                    }
                    bias -= learningRate * error;
                }
                
                if (epoch % 100 == 0) {
                    System.out.printf("Epoch %d: Loss = %.6f, Accuracy = %.2f%%\n", 
                        epoch, totalLoss / n, 100.0 * correct / n);
                }
            }
        }
    }
    
    // ============ UTILITY FUNCTIONS ============
    static class DataUtils {
        static void normalize(double[][] data) {
            int features = data[0].length;
            for (int j = 0; j < features; j++) {
                double mean = 0, std = 0;
                for (double[] row : data) {
                    mean += row[j];
                }
                mean /= data.length;
                
                for (double[] row : data) {
                    std += Math.pow(row[j] - mean, 2);
                }
                std = Math.sqrt(std / data.length);
                
                for (double[] row : data) {
                    row[j] = (row[j] - mean) / (std + 1e-8);
                }
            }
        }
        
        static double[][] splitFeatures(double[][] data, int labelColumn) {
            double[][] features = new double[data.length][data[0].length - 1];
            for (int i = 0; i < data.length; i++) {
                int idx = 0;
                for (int j = 0; j < data[0].length; j++) {
                    if (j != labelColumn) {
                        features[i][idx++] = data[i][j];
                    }
                }
            }
            return features;
        }
        
        static double[] extractLabels(double[][] data, int labelColumn) {
            double[] labels = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                labels[i] = data[i][labelColumn];
            }
            return labels;
        }
    }
    
    // ============ DEMO ============
    public static void main(String[] args) {
        System.out.println("=== Machine Learning Library Demo ===\n");
        
        // ============ LINEAR REGRESSION ============
        System.out.println("=== Linear Regression Demo ===");
        System.out.println("Task: Predict house prices based on size and rooms\n");
        
        double[][] housingData = {
            {1000, 2, 200000}, {1500, 3, 250000}, {2000, 4, 300000},
            {2500, 5, 350000}, {3000, 6, 400000}, {800, 1, 150000},
            {1200, 2, 220000}, {1800, 3, 280000}, {2200, 4, 320000}
        };
        
        double[][] X_linear = DataUtils.splitFeatures(housingData, 2);
        double[] y_linear = DataUtils.extractLabels(housingData, 2);
        DataUtils.normalize(X_linear);
        
        LinearRegression linReg = new LinearRegression(2, 0.01);
        linReg.train(X_linear, y_linear, 1000);
        
        System.out.println("\nTesting predictions:");
        for (int i = 0; i < 3; i++) {
            double pred = linReg.predict(X_linear[i]);
            System.out.printf("Actual: %.0f, Predicted: %.0f\n", y_linear[i], pred);
        }
        
        // ============ LOGISTIC REGRESSION ============
        System.out.println("\n=== Logistic Regression Demo ===");
        System.out.println("Task: Binary classification (pass/fail based on study hours)\n");
        
        double[][] studyData = {
            {1, 0}, {2, 0}, {3, 0}, {4, 1}, {5, 1},
            {6, 1}, {7, 1}, {8, 1}, {1.5, 0}, {2.5, 0},
            {3.5, 0}, {4.5, 1}, {5.5, 1}, {6.5, 1}, {7.5, 1}
        };
        
        double[][] X_logistic = DataUtils.splitFeatures(studyData, 1);
        double[] y_log_d = DataUtils.extractLabels(studyData, 1);
        int[] y_logistic = Arrays.stream(y_log_d).mapToInt(d -> (int) d).toArray();
        
        LogisticRegression logReg = new LogisticRegression(1, 0.1);
        logReg.train(X_logistic, y_logistic, 1000);
        
        System.out.println("\nTesting classifications:");
        double[] testInputs = {{2}, {5}, {7}};
        for (double[] input : testInputs) {
            System.out.printf("Study hours: %.1f, Probability: %.2f, Prediction: %s\n",
                input[0], logReg.predict(input), logReg.classify(input) == 1 ? "Pass" : "Fail");
        }
        
        // ============ NEURAL NETWORK ============
        System.out.println("\n=== Neural Network Demo ===");
        System.out.println("Task: Learn XOR function\n");
        
        List<Matrix> inputs = Arrays.asList(
            Matrix.fromArray(new double[]{0, 0}),
            Matrix.fromArray(new double[]{0, 1}),
            Matrix.fromArray(new double[]{1, 0}),
            Matrix.fromArray(new double[]{1, 1})
        );
        
        List<Matrix> targets = Arrays.asList(
            Matrix.fromArray(new double[]{0}),
            Matrix.fromArray(new double[]{1}),
            Matrix.fromArray(new double[]{1}),
            Matrix.fromArray(new double[]{0})
        );
        
        NeuralNetwork nn = new NeuralNetwork(0.5);
        nn.addLayer(2, 4, new Sigmoid());
        nn.addLayer(4, 1, new Sigmoid());
        
        nn.trainBatch(inputs, targets, 1000);
        
        System.out.println("\nTesting XOR predictions:");
        for (int i = 0; i < inputs.size(); i++) {
            Matrix output = nn.predict(inputs.get(i));
            double[] input = inputs.get(i).toArray();
            System.out.printf("Input: [%.0f, %.0f] -> Output: %.4f (Target: %.0f)\n",
                input[0], input[1], output.data[0][0], targets.get(i).data[0][0]);
        }
        
        // ============ MULTI-LAYER NETWORK ============
        System.out.println("\n=== Deep Neural Network Demo ===");
        System.out.println("Task: Non-linear function approximation\n");
        
        List<Matrix> trainInputs = new ArrayList<>();
        List<Matrix> trainTargets = new ArrayList<>();
        
        for (int i = 0; i < 20; i++) {
            double x = i / 20.0;
            trainInputs.add(Matrix.fromArray(new double[]{x}));
            trainTargets.add(Matrix.fromArray(new double[]{Math.sin(x * Math.PI * 2)}));
        }
        
        NeuralNetwork deepNN = new NeuralNetwork(0.1);
        deepNN.addLayer(1, 8, new ReLU());
        deepNN.addLayer(8, 8, new ReLU());
        deepNN.addLayer(8, 1, new Tanh());
        
        deepNN.trainBatch(trainInputs, trainTargets, 2000);
        
        System.out.println("\nTesting sine wave approximation:");
        for (double x = 0; x <= 1; x += 0.2) {
            Matrix input = Matrix.fromArray(new double[]{x});
            Matrix output = deepNN.predict(input);
            double actual = Math.sin(x * Math.PI * 2);
            System.out.printf("x=%.1f: Predicted=%.4f, Actual=%.4f\n", 
                x, output.data[0][0], actual);
        }
        
        System.out.println("\n=== Demo Complete ===");
    }
}
