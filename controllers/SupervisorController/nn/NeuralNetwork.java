package nn;

import utils.Util;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: annapawlicka
 * Date: 02/03/2013
 * Time: 20:25
 * Neural network which transforms the sensory inputs received from the sensors of the e-puck into motor commands.
 * Inputs are scaled to fit the range [0,1] and the neuronal transfer function is chosen to be a hyperbolic tangent
 * (tanh).
 */
public class NeuralNetwork {

    private int weightsNo;
    private int inputsNo;
    private int outputsNo;
    private float[] weights;
    private int weightMin = -1;
    private int weightMax = 1;

    private float[] outputs;

    private double fitness;

    private Random random = new Random();

    public NeuralNetwork(int inputsNo, int outputsNo, int NB_HIDDEN_NEURONS) {

        this.inputsNo = inputsNo;
        this.outputsNo = outputsNo;
        if (NB_HIDDEN_NEURONS == 0) this.weightsNo = inputsNo * outputsNo + outputsNo;
        else this.weightsNo = inputsNo * NB_HIDDEN_NEURONS + NB_HIDDEN_NEURONS + NB_HIDDEN_NEURONS * outputsNo + outputsNo;
        this.weights = new float[weightsNo];
        this.outputs = new float[this.outputsNo];
        this.fitness = -1;
        initialise();
    }

    public void initialise(){

        // Initialise weights
        for(int i=0; i< weightsNo; i++){
            weights[i] = (random.nextFloat() * (weightMax - weightMin) + weightMin);
        }

        // Initialise outputs
        for(int j=0; j< outputsNo; j++){
            outputs[j] = 0.0f;
        }
    }

    /**
     * Method to create an offspring
     *
     * @param parent - parent whose information is being copied over to offspring
     */
    public void copy(NeuralNetwork parent) {

        this.inputsNo = parent.inputsNo;
        this.outputsNo = parent.outputsNo;
        this.outputs = Util.copy(parent.outputs);

    	/* Transformation */
        this.weights = Util.copy(parent.weights);

    }


    /**
     * Method to mutate weights of the neural network
     * @param GENE_MIN          Minimal value of a gene (weight)
     * @param GENE_MAX          Maximal value of a gene (weight)
     * @param gene              Weight to be mutated
     * @param MUTATION_SIGMA    Determines the extent to which a value will be mutated (how far from its current value)
     * @return                  Returns mutated gene
     */
    public float mutate(float GENE_MIN, float GENE_MAX, float gene, double MUTATION_SIGMA) {
        float x1, x2, w, y1;

        do {
            x1 = (random.nextFloat() * (GENE_MAX - GENE_MIN) + GENE_MIN);
            x2 = (random.nextFloat() * (GENE_MAX - GENE_MIN) + GENE_MIN);
            w = x1 * x1 + x2 * x2;
        } while (w > 1.0 || w == 0); // repeat until the value is not 0 or greater than 1.

        y1 = ((float) (gene + MUTATION_SIGMA * x1 * Math.sqrt((-2.0 * Math.log(w)) / w))); // Add to gene

        if (y1 > GENE_MAX) return GENE_MAX;
        if (y1 < GENE_MIN) return GENE_MIN;

        return y1;
    }

    /**
     * Method to crossover two individuals
     * @param ind1       Index of Individual 1
     * @param ind2       Index of Individual 2
     * @param new_ind    New individual that will be updated with crossovered details
     * @param NB_GENES   Number of genes (weights) that the individual consist of
     * @param population Population that is being worked on
     */
    public void crossover(int ind1, int ind2, NeuralNetwork new_ind, float NB_GENES, NeuralNetwork[] population) {

        int crossover_point;

        crossover_point = (int) Math.floor(NB_GENES * random.nextFloat());

        for (int i = 0; i < NB_GENES; i++) {
            if (i <= crossover_point) new_ind.setWeights(i, population[ind1].getWeights()[i]);
            else new_ind.setWeights(i, population[ind2].getWeights()[i]);
        }
    }


    /* Getters and setters */

    public int getWeightsNo(){
        return weightsNo;
    }

    public float[] getWeights(){
        return weights;
    }

    public void setWeights(int index, float weight){
        weights[index] = weight;
    }

    public void setFitness(double f){
        fitness = f;
    }

    public double getFitness(){
        return fitness;
    }

}
