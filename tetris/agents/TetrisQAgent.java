package src.pas.tetris.agents;


// SYSTEM IMPORTS
import java.util.Iterator;
import java.util.List;
import java.util.Random;


// JAVA PROJECT IMPORTS
import edu.bu.tetris.agents.QAgent;
import edu.bu.tetris.agents.TrainerAgent.GameCounter;
import edu.bu.tetris.game.Board;
import edu.bu.tetris.game.Game.GameView;
import edu.bu.tetris.game.minos.Mino;
import edu.bu.tetris.linalg.Matrix;
import edu.bu.tetris.nn.Model;
import edu.bu.tetris.nn.LossFunction;
import edu.bu.tetris.nn.Optimizer;
import edu.bu.tetris.nn.models.Sequential;
import edu.bu.tetris.nn.layers.Dense; // fully connected layer
import edu.bu.tetris.nn.layers.ReLU;  // some activations (below too)
import edu.bu.tetris.nn.layers.Tanh;
import edu.bu.tetris.nn.layers.Sigmoid;
import edu.bu.tetris.training.data.Dataset;
import edu.bu.tetris.utils.Pair;


public class TetrisQAgent
    extends QAgent
{

    public static final double EXPLORATION_PROB = 0.05;

    private Random random;

    public TetrisQAgent(String name)
    {
        super(name);
        this.random = new Random(12345); // optional to have a seed
    }

    public Random getRandom() { return this.random; }

    @Override
    public Model initQFunction()
    {
        // build a single-hidden-layer feedforward network
        // this example will create a 3-layer neural network (1 hidden layer)
        // in this example, the input to the neural network is the
        // image of the board unrolled into a giant vector
        final int numPixelsInImage = Board.NUM_ROWS * Board.NUM_COLS;
        final int hiddenDim = 2 * numPixelsInImage;
        final int outDim = 1;

        Sequential qFunction = new Sequential();
        qFunction.add(new Dense(4, hiddenDim));
        //qFunction.add(new Tanh());
        qFunction.add(new Dense(hiddenDim,hiddenDim/2));
        qFunction.add(new Dense(hiddenDim/2, outDim));

        return qFunction;
    }

    /**
        This function is for you to figure out what your features
        are. This should end up being a single row-vector, and the
        dimensions should be what your qfunction is expecting.
        One thing we can do is get the grayscale image
        where squares in the image are 0.0 if unoccupied, 0.5 if
        there is a "background" square (i.e. that square is occupied
        but it is not the current piece being placed), and 1.0 for
        any squares that the current piece is being considered for.
        
        We can then flatten this image to get a row-vector, but we
        can do more than this! Try to be creative: how can you measure the
        "state" of the game without relying on the pixels? If you were given
        a tetris game midway through play, what properties would you look for?
     */
    @Override
    public Matrix getQFunctionInput(final GameView game,
                                    final Mino potentialAction)
    {
        // AIMS: 0 blocks on screen, low, no holes
        Matrix flattenedImage = null;
        Matrix tetrisArray = null;
        /*try
        {
            flattenedImage = game.getGrayscaleImage(potentialAction).flatten();
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }*/
        try {
            tetrisArray =game.getGrayscaleImage(potentialAction);
        } catch (Exception e){}
        int numcols = tetrisArray.getShape().getNumCols(); //10
        int numrows = tetrisArray.getShape().getNumRows(); //22
        int highestrow = 999;
        int lowestrow = -1;
        int holes= 0;
        int totalscore = game.getTotalScore();
        //int zscore = game.get
        for (int i=0;i<numrows;i++){ //top to bottom
            int count = 0;
            for (int j=0;j<numcols;j++){ //left to right
                if (tetrisArray.get(i,j)!= 0.0){
                    if (highestrow == 999){
                        highestrow = i-1;
                    }else{
                        count++;
                    }
                    if (i>highestrow && tetrisArray.get(i,j)==0.0){//Below highest row + empty
                        if (tetrisArray.get(i+1,j) !=0.0 && tetrisArray.get(i-1,j)!=0.0 && tetrisArray.get(i,j+1)!=0.0 && tetrisArray.get(i,j-1)!=0.0){
                            holes++;
                        }
                    }
                } 
            }
            if (count >1){
                lowestrow = i;
            }
        }
        int heightdifferential = lowestrow-highestrow;
        //System.out.println(numrows);
        heightdifferential = 22-heightdifferential;
        Matrix finaloutput = null;
        finaloutput = finaloutput.zeros(1,4);
        finaloutput.set(0,0,heightdifferential); //Height difference
        finaloutput.set(0,1,-holes); //Number of holes
        finaloutput.set(0,2,totalscore); //Total score at state
        finaloutput.set(0,3,highestrow); //Highest point in stack

        return finaloutput;
    }

    /**
     * This method is used to decide if we should follow our current policy
     * (i.e. our q-function), or if we should ignore it and take a random action
     * (i.e. explore).
     *
     * Remember, as the q-function learns, it will start to predict the same "good" actions
     * over and over again. This can prevent us from discovering new, potentially even
     * better states, which we want to do! So, sometimes we should ignore our policy
     * and explore to gain novel experiences.
     *
     * The current implementation chooses to ignore the current policy around 5% of the time.
     * While this strategy is easy to implement, it often doesn't perform well and is
     * really sensitive to the EXPLORATION_PROB. I would recommend devising your own
     * strategy here.
     */
    @Override
    public boolean shouldExplore(final GameView game,
                                 final GameCounter gameCounter)
    {
        //long count1 = gameCounter.getTotalGamesPlayed;
        long gamez = gameCounter.getTotalGamesPlayed();
        int min = 0;
        int max = 100;
        Random rand = new Random();
        int randonumb= rand.nextInt(max-min+1);
        boolean explore = false;
        if (gamez<500){
            if (randonumb <7){
                explore=true;
            }
        }
        if(2500>gamez && gamez>500){
            if (randonumb<5){
                explore=true;
            }
        }
        if(4000<=gamez && gamez<=2500){
            if (randonumb < 3){
                explore = true;
            }
        }
        if (gamez > 4000){
            if (randonumb<2){
                explore = true;
            }
        }
        //System.out.println(randonumb);
        if (explore==true){
            //System.out.println(randonumb);
        }

        return explore;
    }

    /**
     * This method is a counterpart to the "shouldExplore" method. Whenever we decide
     * that we should ignore our policy, we now have to actually choose an action.
     *
     * You should come up with a way of choosing an action so that the model gets
     * to experience something new. The current implemention just chooses a random
     * option, which in practice doesn't work as well as a more guided strategy.
     * I would recommend devising your own strategy here.
     */
    @Override
    public Mino getExplorationMove(final GameView game)
    {
        int randIdx = this.getRandom().nextInt(game.getFinalMinoPositions().size());
        List<Mino> xdxx = game.getFinalMinoPositions();
        Mino result = xdxx.get(randIdx);
        for (int k=0;k<xdxx.size();k++){
            Mino dd = xdxx.get(k);
            Matrix greysc = null;
            try{greysc = game.getGrayscaleImage(dd);
            }catch (Exception e){}
            int holes = 0;
            for (int i=0;i<22;i++){ //top to bottom
                int highestrow =999;
                for (int j=0;j<10;j++){ //left to right
                    if (greysc.get(i,j)!= 0.0){
                        if (highestrow == 999){
                            highestrow = i-1;
                        }
                        if (i>highestrow && greysc.get(i,j)==0.0){//Below highest row + empty
                            if (greysc.get(i+1,j) !=0.0 && greysc.get(i-1,j)!=0.0 && greysc.get(i,j+1)!=0.0 && greysc.get(i,j-1)!=0.0){
                                holes++;
                            }
                        }
                    } 
                }
        
            }
            if (holes==0){
                result = xdxx.get(k);
            }
        }
        return result;
    }

    /**
     * This method is called by the TrainerAgent after we have played enough training games.
     * In between the training section and the evaluation section of a phase, we need to use
     * the exprience we've collected (from the training games) to improve the q-function.
     *
     * You don't really need to change this method unless you want to. All that happens
     * is that we will use the experiences currently stored in the replay buffer to update
     * our model. Updates (i.e. gradient descent updates) will be applied per minibatch
     * (i.e. a subset of the entire dataset) rather than in a vanilla gradient descent manner
     * (i.e. all at once)...this often works better and is an active area of research.
     *
     * Each pass through the data is called an epoch, and we will perform "numUpdates" amount
     * of epochs in between the training and eval sections of each phase.
     */
    @Override
    public void trainQFunction(Dataset dataset,
                               LossFunction lossFunction,
                               Optimizer optimizer,
                               long numUpdates)
    {
        for(int epochIdx = 0; epochIdx < numUpdates; ++epochIdx)
        {
            dataset.shuffle();
            Iterator<Pair<Matrix, Matrix> > batchIterator = dataset.iterator();

            while(batchIterator.hasNext())
            {
                Pair<Matrix, Matrix> batch = batchIterator.next();

                try
                {
                    Matrix YHat = this.getQFunction().forward(batch.getFirst());

                    optimizer.reset();
                    this.getQFunction().backwards(batch.getFirst(),
                                                  lossFunction.backwards(YHat, batch.getSecond()));
                    optimizer.step();
                } catch(Exception e)
                {
                    e.printStackTrace();
                    System.exit(-1);
                }
            }
        }
    }

    /**
     * This method is where you will devise your own reward signal. Remember, the larger
     * the number, the more "pleasurable" it is to the model, and the smaller the number,
     * the more "painful" to the model.
     *
     * This is where you get to tell the model how "good" or "bad" the game is.
     * Since you earn points in this game, the reward should probably be influenced by the
     * points, however this is not all. In fact, just using the points earned this turn
     * is a **terrible** reward function, because earning points is hard!!
     *
     * I would recommend you to consider other ways of measuring "good"ness and "bad"ness
     * of the game. For instance, the higher the stack of minos gets....generally the worse
     * (unless you have a long hole waiting for an I-block). When you design a reward
     * signal that is less sparse, you should see your model optimize this reward over time.
     */
    @Override
    public double getReward(final GameView game)
    {
        Matrix tetrisArray = null;
        List<Mino> dxd = game.getFinalMinoPositions();
        Mino potentialAction = dxd.get(0);
        /*try
        {
            flattenedImage = game.getGrayscaleImage(potentialAction).flatten();
        } catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }*/
        try {
            tetrisArray =game.getGrayscaleImage(potentialAction);
        } catch (Exception e){}
        int numcols = tetrisArray.getShape().getNumCols(); //10
        int numrows = tetrisArray.getShape().getNumRows(); //22
        int highestrow = 999;
        int lowestrow = -1;
        int holes= 0;
        int totalscore = game.getTotalScore();
        for (int i=0;i<numrows;i++){ //top to bottom
            int count = 0;
            for (int j=0;j<numcols;j++){ //left to right
                if (tetrisArray.get(i,j)!= 0.0){
                    if (highestrow == 999){
                        highestrow = i-1;
                    }else{
                        count++;
                    }
                    if (i>highestrow && tetrisArray.get(i,j)==0.0){//Below highest row + empty
                        if (tetrisArray.get(i+1,j) !=0.0 && tetrisArray.get(i-1,j)!=0.0 && tetrisArray.get(i,j+1)!=0.0 && tetrisArray.get(i,j-1)!=0.0){
                            holes++;
                        }
                    }
                } 
            }
            if (count >1){
                lowestrow = i;
            }
        }
        int heightdifferential = lowestrow-highestrow;
        int currscore = game.getScoreThisTurn();
        double finalscore = heightdifferential*(-30) + holes*(-30) + totalscore*(45) + highestrow*(15) + currscore*(50);

        return finalscore;
    }

}
