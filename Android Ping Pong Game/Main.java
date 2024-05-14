public class Main {
    // maxX = 1280, maxY = 720

    int ballX = 640; // Declare variable to hold x position of the ball
    int ballY = 360; // Declare variable to hold y position of the ball
    int ballWidth = 30; // Declare variable to hold ball width
    int ballHeight = 30; // Declare variable to hold ball height

    int ballX1 = 640; // Declare variable to hold x position of the ball
    int ballY1 = 360; // Declare variable to hold y position of the ball
    int ballWidth1 = 30; // Declare variable to hold ball width
    int ballHeight1 = 30; // Declare variable to hold ball height

    int size = 50; // Declare to hold text size value

    int playerX1 = 400; // Declare to hold player one score x position
    int playerY1 = 300; // Declare to hold player one score y position
    int playerX2 = 800; // Declare to hold player two score x position
    int playerY2 = 300; // Declare to hold player two score y position

    int scoreOne = 0; // Declare player one score
    int scoreTwo = 0; // Declare player two score

    int xSpeed = 5; // ball's x speed
    int ySpeed = 5; // ball's y speed

    float screenWidth = 1280; // set to your maxX
    float screenHeight = 720; // set to your maxY

    float paddleX = 0; // sets paddle width
    float paddleY = 0; // y position of the left paddle
    float paddleY1 = 515; // y position of the right paddle
    float paddleX1 = 1220; // x position of the left paddle
    float paddleWidth = 50; // width of paddle
    float paddleHeight = 200; // height of paddle

    float diameter = 30; // diameter of the ball
    float radius = 15; // since half the diameter is the radius

    boolean checkLeftPaddle = false;
    boolean checkRightPaddle = false;

    boolean gameOn = false; // sets gameOn to be false when sketch is open

    void setup() {
        fullScreen(); // Sets the program to run in full screen mode
    }

    void draw() {
        background(0); // sets background to black

        movePaddle(); // move padde vertically
        movePaddle1(); // move right paddle vertically
        displayBall(); // draw ball
        displayPaddles(); // draw paddles
        displayScores(); // display both player scores
        moveBall(); // moves ball when screen is touched
        checkWall(); // lets ball bounce off top and bottom walls and sets game to off when ball
                     // touches opposing walls
        setGameMode(); // starts the game if screen is touched
        checkLeftPaddle(); // lets ball bounce off left paddle
        checkRightPaddle(); // lets ball bounce off right paddle

        stroke(0, 250, 0); // sets the colour of the outline of any shape drawn below this code to green
        fill(100, 0, 0); // sets the colour of shapes drawn below to red
        rect(paddleX, paddleY, paddleWidth, paddleHeight); // draws top left paddle
        rect(paddleX1, paddleY1, paddleWidth, paddleHeight); // draws bottom right paddle

        fill(55, 100, 0); // sets outline color of shapes below to green

        textSize(size); // sets text size to 50
        fill(255); // sets text colour to white
        text(scoreOne, playerX1, playerY1); // playerOne score display
        text(scoreTwo, playerX2, playerY2); // playerTwo score display
    }

    // Checks if ball overlaps paddle
    boolean doesOverlap(float paddleX, float paddleY, float paddleWidth, float paddleHeight, float ballX, float ballY,
            float radius) {

        float ballLeftEdge = ballX - radius; // left edge of ball
        float ballBottomEdge = ballY + radius; // bottom edge of ball
        float ballRightEdge = ballX + radius; // right edge of ball
        float ballTopEdge = ballY - radius; // top edge of ball

        float paddleLeftEdge = paddleX; // left edge of paddle
        float paddleBottomEdge = paddleY + paddleHeight; // bottom edge of paddle
        float paddleRightEdge = paddleX + paddleWidth; // right edge of paddle
        float paddleTopEdge = paddleY; // top edge of paddle

        if (ballBottomEdge >= paddleTopEdge // Check if bottom edge of ball and top edge of paddle overlap
                && ballTopEdge <= paddleBottomEdge // Check if top edge of ball and bottom edge of paddle overlap
                && ballLeftEdge <= paddleRightEdge // Check if left edge of ball and right edge of paddle overlap
                && ballRightEdge >= paddleLeftEdge) // Check if right edge of ball and left edge of paddle overlap
        {
            return true;
        } else {
            return false;
        }
    }

    void movePaddle() { // function to move left paddle vertically
        if (mouseX < 640) {
            paddleY = constrain(mouseY, 0, screenHeight - paddleHeight);
        } else {
            paddleY1 = constrain(mouseY, 0, screenHeight - paddleHeight);
        }
    }

    void movePaddle1() { // function to move right paddle vertically
        if (mouseX > 640) {
            paddleY1 = constrain(mouseY, 0, screenHeight - paddleHeight);
        } else {
            paddleY = constrain(mouseY, 0, screenHeight - paddleHeight);
        }
    }

    void displayScores() { // function to display of scores
        // Increment of scores
        if (ballX - radius <= 0) {
            scoreTwo = scoreTwo + 1;// increasing score of player two if player two scores
        }
        if ((ballX + radius) >= screenWidth) {
            scoreOne = scoreOne + 1; // increasing score of player one if player one scores
        }
    }

    // Draws the ball
    void displayBall() { // function to draw ball
        fill(55, 100, 0); // sets outline color of shapes below to green
        ellipse(ballX, ballY, ballWidth, ballHeight); // draws ellipse
    }

    void displayPaddles() { // function to draw paddles
        stroke(0, 250, 0); // sets the colour of the outline of any shape drawn below this code to green
        fill(100, 0, 0); // sets the colour of shapes drawn below to red
        rect(paddleX, paddleY, paddleWidth, paddleHeight); // draws top left paddle
        rect(paddleX1, paddleY1, paddleWidth, paddleHeight); // draws bottom right paddle
    }

    void moveBall() { // function to move ball
        // Move ball
        if (gameOn) {
            ballX = ballX - xSpeed;
            ballY = ballY - ySpeed;
        } else { // return ball to middle
            ballX = ballX1;
            ballY = ballY1;
            ballWidth = ballWidth1;
            ballHeight = ballHeight1;
        }
    }

    void checkWall() { // function to bounce ball of top and bottom walls & to end game if ball hits
                       // lefts or right walls
        // Check if ball hits lefts or right walls
        if ((ballX - radius <= 0) || (ballX + radius) >= screenWidth) {
            gameOn = false; // game ends after hitting opposing walls
        }
        if ((ballY - radius <= 0) || (ballY + radius) >= screenHeight) {
            ySpeed = ySpeed * -1; // reverse direction
        }
    }

    void setGameMode() { // function to start the game
        if (mousePressed) { // sets game to be on if screen is touched
            gameOn = true;
        }
    }

    void checkLeftPaddle() { // function to bounce ball if it hits left paddle
        // Check if there is an overlap between ball and left paddle
        checkLeftPaddle = doesOverlap(paddleX, paddleY, paddleWidth, paddleHeight, ballX, ballY, radius);
        if (checkLeftPaddle == true) {
            xSpeed = xSpeed * -1; // reverses ball direction
        }
    }

    void checkRightPaddle() { // function to bounce ball if it hits right paddle
        // Check if there is an overlap between ball and right paddle
        checkRightPaddle = doesOverlap(paddleX1, paddleY1, paddleWidth, paddleHeight, ballX, ballY, radius);
        if (checkRightPaddle == true) {
            xSpeed = xSpeed * -1; // reverses ball direction
        }
    }
}
