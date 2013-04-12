import com.cyberbotics.webots.controller.*;
import nn.NeuralNetwork;
import utils.FilesFunctions;
import utils.Util;

import java.io.*;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: annapawlicka
 * Date: 09/03/2013
 * Time: 14:27
 * Supervisor controller. Controls the evolution of e-puck. Supervisor can reset position of the epuck.
 * Communication with e-puck is done via emitters/receivers. Separate devices for game and neural communication.
 * Evolution is done by elitism, crossover and mutation.
 * Fitness function of agents is how well they learn the games.
 */

//TODO Test reading best indiv

public class SupervisorController extends Supervisor {

    // Devices
    private Emitter emitter;
    private Emitter gameEmitter;
    private Receiver receiver;
    private Receiver gameReceiver;
    private Node epuck;
    private Field fldTranslation;
    private double[] initTranslation;
    private Display groundDisplay;
    private int width, height;
    private double GROUND_X = 0.9;
    private double GROUND_Z = 0.9;
    private int WHITE = 0xFFFFFF;
    private int BLACK = 0x000000;
    private double[] translation;
    private ImageRef toStore;

    private final int TIME_STEP = 128;                      // [ms]

    // Evolution
    private int NN_POP_SIZE;
    private int GAME_POP_SIZE;
    private int SUBSET_SIZE;
    private int NB_INPUTS;
    private int NB_OUTPUTS;
    private int NB_GENES;
    private NeuralNetwork[] populationOfNN;
    private double[] fitnessNN;
    private double[][] fitnessPerGame;
    private double[][] sortedfitnessNN;                     // Population sorted by fitness
    private double ELITISM_RATIO = 0.1;
    private double REPRODUCTION_RATIO = 0.4;                // If not using roulette wheel (truncation selection), we need reproduction ratio
    private double CROSSOVER_PROBABILITY = 0.5;             // Probability of having a crossover
    private double MUTATION_PROBABILITY = 0.1;              // Probability of mutating each weight-value in a genome
    private int GENE_MIN = -1;                              // Range of genes: minimum value
    private int GENE_MAX = 1;                               // Range of genes: maximum value
    private double MUTATION_SIGMA = 0.2;                    // Mutations follow a Box-Muller distribution from the gene with this sigma
    private int evaluatedNN = 0;                            // Evaluated individuals
    private int generation = 0;                             // Generation counter
    //If 1, evolution takes place. If 0, then the best individual obtained during the previous evolution is tested for an undetermined amount of time.
    private int EVOLVING = 1;
    private int TESTING = 0;
    private int ROULETTE_WHEEL = 1;

    //Log variables
    private double minFitNN = 0.0, avgFitNN = 0.0, bestFitNN = 0.0, absBestFitNN = -10000;
    private double[][] stats;
    private int bestNN = -1, absBestNN = -1;
    private BufferedWriter out1, out2, out3;
    private BufferedReader in1, in2, in3;
    private FileWriter file1, file2, file3;
    private BufferedReader reader1, reader3;

    private Random random = new Random();

    public SupervisorController() {
        super();
    }

    /**
     * This method exits only when simulation is closed/reversed.
     */
    public void run() {

        while (step(TIME_STEP) != -1) {
            byte[] nnFit;
            float finished = -1;

            drawRobotsPosition();

            // As long as individual is being evaluated, print current fitness and return
            int n = receiver.getQueueLength();
            if (n > 0) {
                nnFit = receiver.getData();
                // Convert bytes into floats
                if (nnFit.length == (GAME_POP_SIZE * 4 + 4)) {
                    if (GAME_POP_SIZE == 2) {
                        byte[] game0 = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            game0[i] = nnFit[i];
                        }
                        fitnessPerGame[0][evaluatedNN] = Util.bytearray2float(game0);
                    } else if (GAME_POP_SIZE == 2) {
                        byte[] game0 = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            game0[i] = nnFit[i];
                        }
                        fitnessPerGame[0][evaluatedNN] = Util.bytearray2float(game0);
                        byte[] game1 = new byte[4];
                        int m = 0;
                        for (int k = 4; k < 8; k++) {
                            game1[m] = nnFit[k];
                            m++;
                        }
                        fitnessPerGame[1][evaluatedNN] = Util.bytearray2float(game1);
                    } else if (GAME_POP_SIZE == 3) {
                        byte[] game0 = new byte[4];
                        for (int i = 0; i < 4; i++) {
                            game0[i] = nnFit[i];
                        }
                        fitnessPerGame[0][evaluatedNN] = Util.bytearray2float(game0);
                        byte[] game1 = new byte[4];
                        int m = 0;
                        for (int k = 4; k < 8; k++) {
                            game1[m] = nnFit[k];
                            m++;
                        }
                        fitnessPerGame[1][evaluatedNN] = Util.bytearray2float(game1);
                        byte[] game2 = new byte[4];
                        int l = 0;
                        for (int j = 8; j < 12; j++) {
                            game2[l] = nnFit[j];
                            l++;
                        }
                        fitnessPerGame[2][evaluatedNN] = Util.bytearray2float(game2);
                    }
                    if (GAME_POP_SIZE == 1) {
                        byte[] flag = new byte[4];
                        int p = 0;
                        for (int o = 4; o < 8; o++) {
                            flag[p] = nnFit[o];
                            p++;
                        }
                        finished = Util.bytearray2float(flag);
                    } else if (GAME_POP_SIZE == 2) {
                        byte[] flag = new byte[4];
                        int p = 0;
                        for (int o = 8; o < 12; o++) {
                            flag[p] = nnFit[o];
                            p++;
                        }
                        finished = Util.bytearray2float(flag);
                    } else if (GAME_POP_SIZE == 3) {
                        byte[] flag = new byte[4];
                        int p = 0;
                        for (int o = 12; o < 16; o++) {
                            flag[p] = nnFit[o];
                            p++;
                        }
                        finished = Util.bytearray2float(flag);
                    }
                    receiver.nextPacket();
                } else if (nnFit.length == 4) {
                    byte[] flag = new byte[4];
                    for (int j = 0; j < 4; j++) {
                        flag[j] = nnFit[j];
                    }
                    finished = Util.bytearray2float(flag);
                    receiver.nextPacket();
                }
            }

            // When evaluation is done, an extra flag is returned in the message
            if (finished == 1.0f) {

                if (EVOLVING == 1) {
                    storeImage(evaluatedNN);
                    resetDisplay();

                    // Perform Multi-objective optimisation and create new generation
                    //optimiseAndCreateNewPop();

                    // VEGA based optimisation
                    createNewVEGApopulation();

                    // Rank populationOfNN, select best individuals and create new generation
                    //createNewPopulation();

                    generation++;
                    System.out.println("\nGENERATION \n" + generation);
                    evaluatedNN = 0;
                    avgFitNN = 0.0;
                    bestFitNN = 0;
                    bestNN = 0;
                    minFitNN = 0;

                    resetRobotPosition();
                    // Evolve games every 4 NN generations (gives them time to learn)
                    if (generation % 5 == 0) {
                        // Send flag to start evolution of games
                        byte[] flag = {1};
                        gameEmitter.send(flag);
                    }

                    // Send new weights
                    byte[] msgInBytes = Util.float2Byte(populationOfNN[evaluatedNN].getWeights());
                    emitter.send(msgInBytes);
                }
            } else if (finished == 0.0) {
                if ((evaluatedNN + 1) < NN_POP_SIZE) {
                    storeImage(evaluatedNN);
                    resetDisplay();
                    evaluatedNN++;
                    System.out.println("Evaluated individual " + evaluatedNN);
                    // Send next genome to experiment
                    resetRobotPosition();
                    byte[] msgInBytes = Util.float2Byte(populationOfNN[evaluatedNN].getWeights());
                    emitter.send(msgInBytes);
                }
            }
            if (TESTING == 2) { // Send weights of best individual
                float[] msg = new float[populationOfNN[0].getWeightsNo() + 1];
                for (int i = 0; i < populationOfNN[0].getWeightsNo(); i++) {
                    msg[i] = populationOfNN[0].getWeights()[i];
                }
                msg[populationOfNN[0].getWeightsNo()] = 2.0f; // send flag
                byte[] msgInBytes = Util.float2Byte(msg);
                emitter.send(msgInBytes);
                System.out.println("Sent best genome for testing.");
                TESTING = -1;
            }
        }
    }

    /**
     * Store screenshot of display node into an image file, append current individual's index to file's name
     *
     * @param indivIndex
     */
    private void storeImage(int indivIndex) {
        toStore = groundDisplay.imageCopy(0, 0, width, height);
        groundDisplay.imageSave(toStore, "screenshots/screenshot" + indivIndex + ".png");
        groundDisplay.imageDelete(toStore);
    }

    /**
     * Draw current robot's position on the display
     */
    private void drawRobotsPosition() {
        translation = fldTranslation.getSFVec3f();
        groundDisplay.setOpacity(0.03);
        groundDisplay.setColor(BLACK);
        groundDisplay.fillOval(
                (int) (height * (translation[2] + GROUND_Z / 2) / GROUND_Z),
                (int) (width * (translation[0] + GROUND_X / 2) / GROUND_X),
                2,
                2);
    }

    /**
     * Reset display node by repainting the background
     */
    private void resetDisplay() {
        groundDisplay.setOpacity(1.0);
        groundDisplay.setColor(WHITE);
        groundDisplay.fillRectangle(0, 0, width, height);
        translation = fldTranslation.getSFVec3f();
    }

    private void normaliseFitnessScore(double[] fitnessScores, int gameNo) {

        double min = 0, max = 0;

        if (gameNo == 0) {
            min = -2500000;
            max = 2500000;
        }
        if (gameNo == 1) {
            min = -200000;
            max = 600;
        }
        if (gameNo == 2) {
            min = -600000;
            max = 600000;
        }

        for (int i = 0; i < fitnessScores.length; i++) {
            double temp = 0;
            try {
                temp = Util.normalize(min, max, fitnessScores[i]);  // add buffer of 0.5
            } catch (Exception e) {
                System.err.println("Error while normalizing: " + e.getMessage());
            }
            fitnessScores[i] = temp;
        }
    }

    /**
     * The reset function is called at the beginning of an evolution.
     */
    public void reset() {

        int i;
        for (i = 0; i < NN_POP_SIZE; i++) fitnessNN[i] = -1;

        if (EVOLVING == 1) {
            // Initialise weights randomly
            initializePopulation();
            System.out.println("NEW EVOLUTION\n");
            System.out.println("GENERATION 0\n");
            resetRobotPosition();

            // Then, send weights of NNs to experiment
            byte[] msgInBytes = Util.float2Byte(populationOfNN[evaluatedNN].getWeights());
            emitter.send(msgInBytes);
        }
        int counter = 0;
        String strLine;
        if (TESTING == 1) { // Test last recorded generation
            try {
                while ((strLine = reader3.readLine()) != null && counter < 50) {
                    String[] weightsStr = strLine.split(",");
                    for (i = 0; i < populationOfNN[counter].getWeightsNo(); i++) {
                        populationOfNN[counter].setWeights(i, Float.parseFloat(weightsStr[i]));
                    }
                    counter++;
                }
            } catch (IOException e) {
                e.getMessage();
            }


            System.out.println("TESTING LAST GENERATION \n");
        }

        if (TESTING == 2) { // Test best individual - whole population will be filled with the same individual's weights
            try {
                while ((strLine = reader1.readLine()) != null) {    // Only one line in a file. TODO check if doesn't throw exception
                    String[] weightsStr = strLine.split(",");
                    for (i = 0; i < NN_POP_SIZE; i++) {
                        for (int j = 0; j < populationOfNN[i].getWeightsNo(); j++) {
                            populationOfNN[i].setWeights(j, Float.parseFloat(weightsStr[j]));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Initiate genes of all individuals randomly
     */
    private void initializePopulation() {
        int i, j;
        for (i = 0; i < NN_POP_SIZE; i++) {
            for (j = 0; j < NB_GENES; j++) {
                // all genes must be in the range of [-1, 1]
                populationOfNN[i].setWeights(j, (float) ((GENE_MAX - GENE_MIN) * random.nextFloat() - (GENE_MAX - GENE_MIN) / 2.0));
            }
        }
    }


    /**
     * Multi-objective optimisation, one-point crossover and mutation
     */
    private void optimiseAndCreateNewPop() {

        // 1. At each generation the population is divided into subpopulations
        // which are evaluated using a particular objective function

        int i, j;
        double[][] stats = new double[GAME_POP_SIZE][3];

        NeuralNetwork[][] subpopulations = new NeuralNetwork[GAME_POP_SIZE][SUBSET_SIZE];

        for (i = 0; i < fitnessPerGame.length; i++) { // loop through games
            double[][] sortedFitness = new double[NN_POP_SIZE][2];
            normaliseFitnessScore(fitnessPerGame[i], i);   // Normalise
            for (j = 0; j < fitnessPerGame[i].length; j++) { // loop through actors
                sortedFitness[j][0] = fitnessPerGame[i][j];    // keep fitness score
                sortedFitness[j][1] = j;                        // keep index
            }
            quickSort(sortedFitness, 0, sortedFitness.length - 1); // sort for current game
            // Find and log current and absolute best individual
            bestFitNN = sortedFitness[0][0];
            minFitNN = sortedFitness[NN_POP_SIZE - 1][0];
            bestNN = (int) sortedFitness[0][1];
            avgFitNN = Util.mean(fitnessPerGame[i]);
            System.out.println("Game: " + i + " stats");
            System.out.println("Best fitness score: " + bestFitNN);
            System.out.println("Average fitness score: " + avgFitNN);
            System.out.println("Worst fitness score: " + minFitNN);
            // Update stats
            stats[i][0] = avgFitNN;
            stats[i][1] = bestFitNN;
            stats[i][2] = minFitNN;
            NeuralNetwork[] subpop = new NeuralNetwork[SUBSET_SIZE];

            for (int k = 0; k < subpop.length; k++) {
                subpop[k] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
            }
            for (int l = 0; l < subpop.length; l++) { // Copy best individuals in a current game
                for (int m = 0; m < subpop[l].getWeightsNo(); m++) {
                    subpop[l].setWeights(m, populationOfNN[(int) sortedFitness[l][1]].getWeights()[m]);
                }
            }
            subpopulations[i] = subpop;

        }
        // Log stats to the files
        FilesFunctions.logPopulation(out1, generation, stats);

        // 2. All subpopulations are shuffled together
        NeuralNetwork[] population = Util.concat(subpopulations);

        NeuralNetwork[] newpop = new NeuralNetwork[NN_POP_SIZE];
        for (i = 0; i < newpop.length; i++) {
            newpop[i] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
        }

        // 3. Create new population and perform crossover
        for (i = 0; i < NN_POP_SIZE; i++) {
            int ind1 = random.nextInt(NN_POP_SIZE);
            // If we will do crossover, select a second individual
            if (random.nextFloat() < CROSSOVER_PROBABILITY) {
                int ind2;
                do {
                    ind2 = random.nextInt(NN_POP_SIZE);
                } while (ind1 == ind2);
                newpop[i].crossover(ind1, ind2, newpop[i], NB_GENES, population);
            } else { //if no crossover was done, just copy selected individual directly
                for (j = 0; j < NB_GENES; j++)
                    newpop[i].setWeights(j, population[ind1].getWeights()[j]);
            }
        }
        // 4. Mutate new populationOfNN and copy back to pop
        for (i = 0; i < NN_POP_SIZE; i++) {
            for (j = 0; j < NB_GENES; j++)
                if (random.nextFloat() < MUTATION_PROBABILITY)
                    populationOfNN[i].setWeights(j, populationOfNN[i].mutate(GENE_MIN, GENE_MAX, newpop[i].getWeights()[j], MUTATION_SIGMA));
                else
                    populationOfNN[i].copy(newpop[i]);
        }

        // Reset fitness
        for (i = 0; i < fitnessPerGame.length; i++) {
            for (j = 0; j < fitnessPerGame[i].length; j++) fitnessPerGame[i][i] = 0;
        }
    }

    /**
     * Fitness proportional selection to create a mating pool
     *
     * @param subpopulation
     * @return
     */
    private double[] rouletteSelect(NeuralNetwork[] subpopulation) {

        // 1. Sort
        double[] fitness = new double[subpopulation.length];
        for (int i = 0; i < fitness.length; i++) fitness[i] = subpopulation[i].getFitness();
        double[][] sortedFitness = new double[subpopulation.length][2];
        sortPopulation(sortedFitness, fitness);

        double total_fitness = 0;
        // 2. Find minimum fitness to subtract it from sum
        double min_fitness = sortedFitness[subpopulation.length - 1][0];
        if (min_fitness < 0) min_fitness = 0;
        int i, j;
        // 3. Calculate total of fitness, used for roulette wheel selection
        for (i = 0; i < subpopulation.length; i++) total_fitness += fitnessNN[i];
        total_fitness -= min_fitness * subpopulation.length;
        // 4. Create mating pool
        double[] pool = new double[subpopulation.length];
        for (i = 0; i < subpopulation.length; i++) {
            int ind = 0;
            float r = random.nextFloat();
            double fitness_counter = (sortedFitness[ind][0] - min_fitness) / total_fitness;
            while (r > fitness_counter && ind < subpopulation.length - 1) {
                ind++;
                fitness_counter += (sortedFitness[ind][0] - min_fitness) / total_fitness;
                if (ind == subpopulation.length - 1) break;
            }
            pool[i] = ind;
        }
        return pool;
    }

    /**
     * Algorithm to perform VEGA Multi-Objective Optimisation
     */
    private void createNewVEGApopulation() {
        int i, j, counter = 0;

        // 1. Shuffle population
        Util.shuffleList(populationOfNN);

        // 2. divide population into O subpopulations of size N/O
        NeuralNetwork[][] subpopulations = new NeuralNetwork[GAME_POP_SIZE][SUBSET_SIZE];
        for (i = 0; i < subpopulations.length; i++) {

            // Create subpopulation
            NeuralNetwork[] subpop = new NeuralNetwork[SUBSET_SIZE];
            for (int k = 0; k < subpop.length; k++) {
                subpop[k] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
            }

            normaliseFitnessScore(fitnessPerGame[i], i); // Normalise fitness scores
            double[][] sortedFitness = new double[NN_POP_SIZE][2];
            for (j = 0; j < fitnessPerGame[i].length; j++) { // loop through actors
                sortedFitness[j][0] = fitnessPerGame[i][j];    // keep fitness score
                sortedFitness[j][1] = j;                        // keep index
            }
            quickSort(sortedFitness, 0, sortedFitness.length - 1); // sort for current game
            // Find and log current and absolute best individual
            bestFitNN = sortedFitness[0][0];
            minFitNN = sortedFitness[NN_POP_SIZE - 1][0];
            bestNN = (int) sortedFitness[0][1];
            avgFitNN = Util.mean(fitnessPerGame[i]);
            if (bestFitNN > stats[i][1]) {
                stats[i][3] = bestNN;
            }
            // Log best individual
            try {
                FilesFunctions.logBestIndiv(populationOfNN, (int) stats[i][3]);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            System.out.println("Game: " + i + " stats");
            System.out.println("Best fitness score: " + bestFitNN);
            System.out.println("Average fitness score: " + avgFitNN);
            System.out.println("Worst fitness score: " + minFitNN);
            System.out.println("Absolute best index: " + stats[i][3]);
            // Update stats
            stats[i][0] = avgFitNN;
            stats[i][1] = bestFitNN;
            stats[i][2] = minFitNN;

            for (int l = 0; l < subpop.length; l++) {
                for (int m = 0; m < subpop[l].getWeightsNo(); m++) {
                    subpop[l].setWeights(m, populationOfNN[counter].getWeights()[m]);
                }
                // 3. Assign fitness to each member in each subpopulation using one objective per subpopulation
                subpop[l].setFitness(fitnessPerGame[i][counter]);
                counter++;
            }

            // 4. Use fitness proportionate selection to create a 'mating pool' for each subpopulation
            double[] probTable = rouletteSelect(subpop);    // add to mating pool in a fitness proportionate way

            NeuralNetwork[] sub = new NeuralNetwork[NN_POP_SIZE / GAME_POP_SIZE];
            for (int k = 0; k < sub.length; k++) {
                sub[k] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
            }
            for (int k = 0; k < sub.length; k++) {
                // randomly select individual from a mating pool
                int r = random.nextInt(probTable.length);
                for (j = 0; j < NB_GENES; j++) {
                    sub[k].setWeights(j, subpop[r].getWeights()[j]);
                }
            }
            // 5. Replace the subpopulation with a mating pool
            for (int k = 0; k < subpopulations[i].length; k++) {
                subpopulations[i][k] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
                for (int l = 0; l < subpopulations[i][k].getWeightsNo(); l++) {
                    subpopulations[i][k].copy(sub[i]);
                }
            }
        }

        // Log stats to the files
        FilesFunctions.logPopulation(out1, generation, stats);

        // 5. Merge 'mating pools' (i.e. the modified subpopulations)
        NeuralNetwork[] tempPopulation = Util.concat(subpopulations);

        // 6. Create new population
        NeuralNetwork[] newpop = new NeuralNetwork[NN_POP_SIZE];
        for (i = 0; i < newpop.length; i++) {
            newpop[i] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
        }

        // 7. Perform crossover
        for (i = 0; i < NN_POP_SIZE; i++) {
            // Copy from temp pop
            int ind1 = random.nextInt(NN_POP_SIZE);
            // If we will do crossover, select a second individual
            if (random.nextFloat() < CROSSOVER_PROBABILITY) {
               int ind2;
                do {
                    ind2 = random.nextInt(NN_POP_SIZE);
                } while (ind1 == ind2);
                newpop[i].crossover(ind1, ind2, newpop[i], NB_GENES, tempPopulation);
            } else { //if no crossover was done, just copy selected individual directly
                for (j = 0; j < NB_GENES; j++)
                    newpop[i].setWeights(j, tempPopulation[ind1].getWeights()[j]);
            }
        }
        // 8. Mutate new population and copy back to pop
        for (i = 0; i < NN_POP_SIZE; i++) {
            for (j = 0; j < NB_GENES; j++)
                if (random.nextFloat() < MUTATION_PROBABILITY)
                    populationOfNN[i].setWeights(j, populationOfNN[i].mutate(GENE_MIN, GENE_MAX, newpop[i].getWeights()[j], MUTATION_SIGMA));
                else
                    populationOfNN[i].copy(newpop[i]);
        }

        // Reset fitness
        for (i = 0; i < fitnessPerGame.length; i++) {
            for (j = 0; j < fitnessPerGame[i].length; j++) fitnessPerGame[i][i] = 0;
        }

    }


    /**
     * Based on the fitness of the last generation, generate a new population of genomes for the next generation.
     */
    private void createNewPopulation() {

        // Sort population
        //normaliseFitnessScore(fitnessNN); // Normalise fitness scores
        // Sort populationOfNN by fitness
        sortPopulation(sortedfitnessNN, fitnessNN);
        // Find and log current and absolute best individual
        bestFitNN = sortedfitnessNN[0][0];
        minFitNN = sortedfitnessNN[NN_POP_SIZE - 1][0];
        bestNN = (int) sortedfitnessNN[0][1];
        avgFitNN = Util.mean(fitnessNN);
        if (bestFitNN > absBestFitNN) {
            absBestFitNN = bestFitNN;
            absBestNN = bestNN;
            FilesFunctions.logBest(out3, generation, NB_GENES, absBestNN, populationOfNN);
        }
        System.out.println("Best fitness score: \n" + bestFitNN);
        System.out.println("Average fitness score: \n" + avgFitNN);
        System.out.println("Worst fitness score: \n" + minFitNN);
        System.out.println("Absolute best index: " + absBestNN);

        // Write data to files
        //FilesFunctions.logPopulation(out1, avgFitNN, generation, fitnessNN,
        //        bestFitNN, minFitNN, NB_GENES, populationOfNN, bestNN);
        FilesFunctions.logAllActorFitnesses(out2, generation, fitnessNN);

        // Log the generation data  - stores weights
        try {
            FilesFunctions.logLastGeneration(populationOfNN);
        } catch (IOException e) {
            e.getMessage();
        }
        // Log best individual
        try {
            FilesFunctions.logBestIndiv(populationOfNN, absBestNN);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        NeuralNetwork[] newpop = new NeuralNetwork[NN_POP_SIZE];
        for (int i = 0; i < newpop.length; i++) {
            newpop[i] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
        }
        double elitism_counter = NN_POP_SIZE * ELITISM_RATIO;
        double total_fitness = 0;

        // Find minimum fitness to subtract it from sum
        double min_fitness = sortedfitnessNN[NN_POP_SIZE - 1][0];
        //if (min_fitness < 0) min_fitness = 0; // Causes an issue if scores are below 0
        int i, j;
        // Calculate total of fitness, used for roulette wheel selection
        for (i = 0; i < NN_POP_SIZE; i++) total_fitness += fitnessNN[i];
        total_fitness -= min_fitness * NN_POP_SIZE;
        // Create new population
        for (i = 0; i < NN_POP_SIZE; i++) {

            // The elitism_counter best individuals are simply copied to the new populationOfNN
            if (i < elitism_counter) {
                for (j = 0; j < NB_GENES; j++)
                    newpop[i].setWeights(j, populationOfNN[(int) sortedfitnessNN[i][1]].getWeights()[j]);
            }
            // The other individuals are generated through the crossover of two parents
            else {

                // Select non-elitist individual
                int ind1 = 0;

                float r = random.nextFloat();
                double fitness_counter = (sortedfitnessNN[ind1][0] - min_fitness) / total_fitness;
                while (r > fitness_counter && ind1 < NN_POP_SIZE - 1) {
                    ind1++;
                    fitness_counter += (sortedfitnessNN[ind1][0] - min_fitness) / total_fitness;
                    if (ind1 == NN_POP_SIZE - 1) break;
                }

                // If we will do crossover, select a second individual
                if (random.nextFloat() < CROSSOVER_PROBABILITY) {
                    int ind2 = 0;
                    do {
                        r = random.nextFloat();
                        fitness_counter = (sortedfitnessNN[ind2][0] - min_fitness) / total_fitness;
                        while (r > fitness_counter && ind2 < NN_POP_SIZE - 1) {
                            ind2++;
                            fitness_counter += (sortedfitnessNN[ind2][0] - min_fitness) / total_fitness;
                            if (ind2 == NN_POP_SIZE - 1) break;
                        }
                    } while (ind1 == ind2);
                    ind1 = (int) sortedfitnessNN[ind1][1];
                    ind2 = (int) sortedfitnessNN[ind2][1];
                    newpop[i].crossover(ind1, ind2, newpop[i], NB_GENES, populationOfNN);
                } else { //if no crossover was done, just copy selected individual directly
                    for (j = 0; j < NB_GENES; j++)
                        newpop[i].setWeights(j, populationOfNN[(int) sortedfitnessNN[ind1][1]].getWeights()[j]);
                }
            }
        }
        // Mutate new populationOfNN and copy back to pop
        for (i = 0; i < NN_POP_SIZE; i++) {
            if (i < elitism_counter) { //no mutation for elitists
                for (j = 0; j < NB_GENES; j++) {
                    populationOfNN[i].copy(newpop[i]);
                }
            } else { // Mutate others with probability per gene
                for (j = 0; j < NB_GENES; j++)
                    if (random.nextFloat() < MUTATION_PROBABILITY)
                        populationOfNN[i].setWeights(j, populationOfNN[i].mutate(GENE_MIN, GENE_MAX, newpop[i].getWeights()[j], MUTATION_SIGMA));
                    else
                        populationOfNN[i].copy(newpop[i]);
            }

            // Reset fitness
            fitnessNN[i] = 0;
        }
    }


    /**
     * Sort whole population according to fitness score of each individual. Uses quickSort.
     */
    private void sortPopulation(double[][] sortedfitness, double[] fitness) {
        int i;
        //sort populationOfNN by fitness
        for (i = 0; i < sortedfitness.length; i++) {
            sortedfitness[i][0] = fitness[i];
            sortedfitness[i][1] = (float) i; //keep index
        }
        quickSort(sortedfitness, 0, sortedfitness.length - 1);
    }

    /**
     * Standard fast algorithm to sort populationOfNN by fitness
     *
     * @param fitness Array that stores fitness and index of each individual.
     * @param left    Min index of the array
     * @param right   Max index of the array
     */
    private void quickSort(double fitness[][], int left, int right) {
        double[] pivot = new double[2];
        int l_hold, r_hold;

        l_hold = left;
        r_hold = right;
        pivot[0] = fitness[left][0];
        pivot[1] = fitness[left][1];
        while (left < right) {
            while ((fitness[right][0] <= pivot[0]) && (left < right))
                right--;
            if (left != right) {
                fitness[left][0] = fitness[right][0];
                fitness[left][1] = fitness[right][1];
                left++;
            }
            while ((fitness[left][0] >= pivot[0]) && (left < right))
                left++;
            if (left != right) {
                fitness[right][0] = fitness[left][0];
                fitness[right][1] = fitness[left][1];
                right--;
            }
        }
        fitness[left][0] = pivot[0];
        fitness[left][1] = pivot[1];
        pivot[0] = left;
        left = l_hold;
        right = r_hold;
        if (left < (int) pivot[0]) quickSort(fitness, left, (int) pivot[0] - 1);
        if (right > (int) pivot[0]) quickSort(fitness, (int) pivot[0] + 1, right);
    }


    /**
     * Resets the position of the epuck before each generation's trials
     */
    private void resetRobotPosition() {
        fldTranslation.setSFVec3f(initTranslation);
    }

    private void initialise() {

        int i, j;

        /* Population/Evolution parameters */
        NN_POP_SIZE = 30;
        GAME_POP_SIZE = 3;
        SUBSET_SIZE = NN_POP_SIZE / GAME_POP_SIZE;
        fitnessPerGame = new double[GAME_POP_SIZE][NN_POP_SIZE];

        // Neural Networks
        NB_INPUTS = 7;
        NB_OUTPUTS = 2;
        NB_GENES = 10;
        populationOfNN = new NeuralNetwork[NN_POP_SIZE];
        for (i = 0; i < NN_POP_SIZE; i++) populationOfNN[i] = new NeuralNetwork(NB_INPUTS, NB_OUTPUTS);
        fitnessNN = new double[NN_POP_SIZE];
        for (i = 0; i < NN_POP_SIZE; i++) fitnessNN[i] = 0.0;
        sortedfitnessNN = new double[NN_POP_SIZE][2];
        for (i = 0; i < sortedfitnessNN.length; i++) {
            for (j = 0; j < 2; j++) {
                sortedfitnessNN[i][j] = 0.0;
            }
        }

        // Initialise stats
        stats = new double[GAME_POP_SIZE][4];
        for (i = 0; i < stats.length; i++) {
            for (j = 0; j < stats[i].length; j++) stats[i][j] = 0;
        }

        // Nodes
        receiver = getReceiver("receiver");
        receiver.enable(TIME_STEP);
        emitter = getEmitter("emitter");
        epuck = getFromDef("EPUCK");
        fldTranslation = epuck.getField("translation");

        gameEmitter = getEmitter("gamesemittersuper");
        gameEmitter.setChannel(1);
        gameReceiver = getReceiver("gamesreceiversuper");
        gameReceiver.setChannel(1);
        gameReceiver.enable(TIME_STEP);

        // Initialise gps coordinates arrays
        initTranslation = new double[3];
        initTranslation = fldTranslation.getSFVec3f();

        // Display
        groundDisplay = getDisplay("ground_display");
        width = groundDisplay.getWidth();
        height = groundDisplay.getHeight();
        // paint the display's background
        resetDisplay();

        // Logging
        try {
            file1 = new FileWriter("results:fitness.txt");
        } catch (IOException e) {
            System.out.println("Cannot open fitness.txt file.");
        }

        out1 = new BufferedWriter(file1);
        try {
            out1.write("Generation");
            for (i = 0; i < GAME_POP_SIZE; i++) {
                out1.write(",Average" + i + ",Best" + i + ",Worst" + i + ",Abs" + i);
            }
            out1.write("\n");
            out1.flush();

        } catch (IOException e) {
            System.out.println("" + e.getMessage());
        }

        try {
            file2 = new FileWriter("all_actor_fit.txt");
        } catch (IOException e) {
            System.err.println("Error while opening file: all_actor_fit.txt " + e.getMessage());
        }

        out2 = new BufferedWriter(file2);

        try {
            out2.write("generation");
            for (i = 0; i < NN_POP_SIZE; i++) {
                out2.write(",Actor" + i + ",");
            }
            out2.write("\n");

        } catch (IOException e) {
            System.out.println("" + e.getMessage());
        }

        try {
            file3 = new FileWriter("results:bestgenome.txt");
        } catch (IOException e) {
            System.out.println("Cannot open bestgenome.txt file.");
        }

        out3 = new BufferedWriter(file3);

        /* Reading from file - for testing purposes */

        try {
            reader3 = new BufferedReader(new FileReader("results:genomes.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Cannot read from file: results:genomes.txt");
            System.out.println(e.getMessage());
        }

        try {
            reader1 = new BufferedReader(new FileReader("best_actor.txt"));
        } catch (FileNotFoundException e) {
            System.out.println("Cannot read from file: best_actor.txt");
            System.out.println(e.getMessage());
        }

        System.out.println("Supervisor has been initialised.");
    }


    public static void main(String[] args) {
        SupervisorController supervisorController = new SupervisorController();
        supervisorController.initialise();
        supervisorController.reset();
        supervisorController.run();
    }

}
