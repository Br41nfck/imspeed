package app;
import static app.Utils.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.obj.CurtainBlock;
import app.obj.ScoreboardEntry;
import app.obj.Word;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import menu.Select;
import menu.Words;

public class Window extends Application
{
	public static Stage window;
	public static long howOften, howFast;
	public static int maxWords, howMany;
	public static int timeLeft;
	public static double multiplierAdd;
	public static List<Integer> xVal, yVal;

	static double points;
	// List of all registered CPMs [for average calculating]
	static final List<Integer> CPMs = new ArrayList<Integer>();
	// Average CPM (for saving)
	static int avgCPM;
	static double totalSeconds;

	private static AnimationTimer animation_words, animation_background, animation_game_over, animation_curtain, game_timer;
	private static boolean curtain, pause = false;
	private static int typedWords, typedChars, maxWordLen = 0;
	private static double multiplier;
	private static long startTime, pauseTime;

	public static int gameDifficulty;
	public static int gameMode;
	public static boolean infinite = false;

	public static void launcher(String[] args)
	{
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception
	{
		window = primaryStage;
		window.getIcons().add(new Image("/resources/img/icon.jpg"));

		Scenes.fontSetup();
		Select.selectGamemode();

		window.setTitle("I'm speed");
		window.setResizable(false);
		window.show();

		// If user didn't confirm, the score remove it from score board file
		window.setOnCloseRequest(e ->
		{
			Log.success("Exiting...");
			if (!Scenes.saved)
			{
				final ScoreboardEntry entry = ScoreboardEntry.activeEntry;
				if (entry != null) Utils.removeRecord(entry.getDate());
			}
		});
	}

	public static void error(String err)
	{
		Scene error = new Scene(Scenes.error(err));
		window.setScene(error);

		error.setOnKeyPressed(e -> {
			switch (e.getCode())
			{
				case ESCAPE:
					Select.selectDifficulty();
					break;
				default:
					break;
			}
		});
	}

	public static void curtain(Scene scene, Pane root)
	{
		 // Stop the CPM timer
		game_timer.stop();
		curtain = true;
		Rectangle cover = new Rectangle(800, 500, Colors.BACKGROUND_C);
		cover.setVisible(false);
		root.getChildren().add(cover);
		List<CurtainBlock> blocks = new ArrayList<>();
		// Generate 10 curtain blocks for each site
		for (int i = 0; i < 10; i++)
		{
			CurtainBlock left_block = new CurtainBlock(i * 50, Math.log(20 - Math.abs(4.5 - i)) * 13, "L");
			blocks.add(left_block);
			CurtainBlock right_block = new CurtainBlock(i * 50, Math.log(20 - Math.abs(4.5 - i)) * 13, "R");
			blocks.add(right_block);
			root.getChildren().addAll(left_block, right_block);
		}
		blocks.forEach(block -> block.toFront());
		animation_curtain = new AnimationTimer()
		{
			private long lastUpdate = 0;
			boolean reverse = false;
			@Override
			public void handle(long now)
			{
				if (now - lastUpdate >= 35_000_000)
				{
					if (!reverse && blocks.get(0).getWidth() > 450)
						{
							animation_background.stop();
							animation_words.stop();
							reverse = true;
							cover.setVisible(true);
							cover.toFront();
							blocks.forEach(block -> block.toFront());
						}
						else blocks.forEach(block -> block.moveToMiddle());
						
					}
					else
					{
						if (blocks.get(8).getWidth() < 1)
						{
							animation_curtain.stop();
							gameOver();
							return;
						}
						else blocks.forEach(block -> block.moveOutside());
						
					}
					lastUpdate = now;
				}
		};
		animation_curtain.start();
	}

	// Fallback function
	public static void gameOver()
	{
		gameOver(false);
	}

	public static void gameOver(boolean closed)
	{
		if (CPMs.size() > 0)
		{
			for (int c : CPMs) avgCPM += c;
			avgCPM /= CPMs.size();
		}
		else avgCPM = 0;
	
		// Don't log on close
		if (!closed)
		{
			System.out.println();
			Log.warning("[GAME OVER]");
			final String[] t = Utils.formatTimePlayed(totalSeconds);
			Log.success(String.format("Total game time: %s:%s:%s", t[0], t[1], t[2]));
		}

		if (points > 0) Utils.saveScore(Scenes.pointsVal.getText());
		
		// Show "Game Over" screen
		Pane root = Scenes.gameOver();
		root.setPrefSize(800, 500);

		Text retry = new Text("> Press SPACE to try again <");
		retry.setFill(Color.WHITE);
		retry.setTranslateX(308);
		retry.setTranslateY(370);
		retry.setFont(Font.font("Courier new", 20));

		root.getChildren().add(retry);
		root.setOpacity(0);

		Scene scene = new Scene(root);
		scene.setFill(Colors.BACKGROUND_C);

		window.setScene(scene);
		Utils.fadeIn(root, 300);

		animation_game_over = blinkingNodeTimer(retry);
		animation_game_over.start();

		scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent e) -> {
			if (e.getCode() == KeyCode.SPACE)
			{
				animation_game_over.stop();
				System.out.println();
				window.setScene(Scenes.scoreboard());
			}
			e.consume();
		});
	};
	
	public static void startGame(List<File> selected)
	{
		Pane root = Scenes.game();
		Scene scene = new Scene(root);

		// RESET EVERYTHING
		curtain = false;
		typedWords = 0;
		typedChars = 0;
		multiplier = 0.98;
		points = -7;
		CPMs.clear();

		Scenes.CPM.setText("0");
		Scenes.pointsVal.setText("0");
		// List of all word-strings combined
		List<String> strings = Words.loadWords(selected);
		// List of active words
		List<Word> words = new ArrayList<Word>();
		// List of new words [for placement optimization]
		List<Word> fresh = new ArrayList<Word>();

		// Get longest word's length
		for (String s : strings)
			if (s.length() > maxWordLen) maxWordLen = s.length();
		// Multiply by pixels of 1 letter with space
		maxWordLen *= 9;

		// List for predefined x & y coordinates
		final List<Integer> xVal_final = new ArrayList<Integer>();
		final List<Integer> yVal_final = new ArrayList<Integer>();

		// Predefined values
		for (int i = -10; i < 10; i += 5)  xVal_final.add(i);
		for (int i = 20; i < 400; i += 20) yVal_final.add(i);

		// Temporary sub lists
		xVal = new ArrayList<Integer>(xVal_final);
		yVal = new ArrayList<Integer>(yVal_final);

		// First word on screen
		Word first = new Word(0, 195, "helloworld");
		words.add(first);

		root.getChildren().add(first);
		// Render scene
		window.setScene(scene);

		// Set difficulty variables
		switch (gameDifficulty)
		{
			case 1:
				maxWords = 16;
				multiplierAdd = 0.01;
				howOften = 7_000_000_000l;
				howFast = 1_650_000_000;
				howMany = 3;
				timeLeft = 25;
				break;

			case 2:
				maxWords = 17;
				multiplierAdd = 0.03;
				howOften = 6_000_000_000l;
				howFast = 750_000_000;
				howMany = 5;
				timeLeft = 15;
				break;

			case 3:
				maxWords = 17;
				multiplierAdd = 0.05;
				howOften = 5_500_000_000l;
				howFast = 650_000_000;
				howMany = 6;
				timeLeft = 10;
				break;

			case 4:
				maxWords = 18;
				multiplierAdd = 0.1;
				howOften = 4_500_000_000l;
				howFast = 550_000_000;
				howMany = 6;
				timeLeft = 5;
				break;

			case 5:
				maxWords = 100;
				break;

		}

		Scenes.conditionVal.setText(Window.gameMode == 0 ? "0" : String.valueOf(timeLeft));

		// Timer for calculating CPM
		game_timer = new AnimationTimer()
		{
			private long lastUpdate = 0;

			@Override
			public void handle(long now)
			{
				if (pause) lastUpdate = now;

				// every 1s
				if (now - lastUpdate >= 1_000_000_000)
				{
					if (gameMode == 1)
					{
						Scenes.conditionVal.setText(String.valueOf(--timeLeft));
						if (timeLeft <= 0)
						{
							// Remove all objects
							root.getChildren().removeAll(words);
							curtain(scene, root);
						}
					}

					// Calculating CPM
					totalSeconds = (now - startTime) / 1_000_000_000l;
					double minutes = totalSeconds / 60;
					int calc = (int) Math.round(typedChars / minutes);

					// Skip the first word
					if (calc > -1 && typedWords > 1)
					{
						CPMs.add(calc);
						Scenes.CPM.setText(String.valueOf(calc));
					}
						else
						{
							// Ranges for color change
							if (calc > 350)
							 	// >350
								Scenes.CPM.setStyle(Colors.GOLD_GRADIENT);
							else
								Scenes.CPM.setFill(
										// 250-350
										(calc > 250) ? Colors.GREEN_C :
										 		// 200-250
												(calc > 200) ? Colors.YELLOW_C :
														// 150-200
														(calc > 150) ? Colors.ORANGE_C :
																// <150
																Colors.RED_C
								);
						}
					}
					lastUpdate = now;
				}
			};
		
		animation_background = Utils.getBackgroundTimer(790, 398, root);
		animation_background.start();

		// Animating words
		animation_words = new AnimationTimer()
		{
			private long lastUpdate = 0;
			private long lastUpdate2 = 0;

			int strike = 0;

			@Override
			public void handle(long now)
			{
				if (now - lastUpdate >= howFast)
				{
					// List of words to deletion after loop
					List<Word> del = new ArrayList<Word>();
					boolean gameOver = false;

					outer_loop: for (Word w : words)
					{
						if (curtain) break;
						// Move all words forward
						w.moveForward();
						w.toFront();

						double xPos = w.getTranslateX();
						if (xPos > 805)
						{ 	// If word leaves beyond the window, reset multiplier
							multiplier = 1;
							// Remove word from the pane
							root.getChildren().remove(w);
							// Add word to deletion list
							del.add(w);

							switch (gameMode)
							{
								case 0:
									// Update missed and increase strikes
									Scenes.conditionVal.setText(String.valueOf(++strike));
									if (typedWords != 0) Log.warning("[STRIKE]: " + strike);
									
									if (!infinite && strike >= 10)
									{
										gameOver = true;
										break outer_loop;
									}
									break;

								case 1:
									timeLeft -= 10;
									if (timeLeft <= 0)
									{
										gameOver = true;
										break outer_loop;
									}
									else Scenes.conditionVal.setText(String.valueOf(timeLeft));
									break;
							}
						}
						// If word is further than longest word remove it from list of new words
						if (xPos > maxWordLen) fresh.remove(w);
					}

					if (gameOver)
					{
						// Remove all objects
						root.getChildren().removeAll(words);
						curtain(scene, root);
					}
					else words.removeAll(del);
					
					// If no words are on the screen and it's not the end of the game
					if (words.isEmpty())
					{
						if (!curtain && typedWords == 0) curtain(scene, root);
						else
						{
							// Else generate new words
							for (int i = 0; i < howMany; i++)
							{
								Word word = createWord(strings, xVal_final, yVal_final, fresh);
								fresh.add(word);
								words.add(word);
								root.getChildren().add(word);
							}
						}
					}
					lastUpdate = now;
				}

				// Every [n] seconds add [m] new words if less than [x] are displayed
				if (now - lastUpdate2 >= howOften && typedWords > 4)
				{
					for (int i = 0; i < howMany; i++)
					{
						if (words.size() < maxWords)
						{
							Word word = createWord(strings, xVal_final, yVal_final, fresh);
							fresh.add(word);
							words.add(word);
							root.getChildren().add(word);
						}
					}
					lastUpdate2 = now;
				}
			}
		
		};
		animation_words.start();

		Scenes.input.setOnKeyPressed(e -> {

			switch (e.getCode())
			{
				case ESCAPE:

					if (!pause)
					{
						animation_words.stop();
						pauseTime = System.nanoTime();

						Log.success("Suspended");
						Scenes.input.setEditable(false);
						Scenes.pauseBox.setVisible(true);
						Scenes.pauseBox.toFront();
					}
					else
					{
						animation_words.start();
						startTime = System.nanoTime() - (pauseTime - startTime);
						Scenes.input.setEditable(true);
						Scenes.pauseBox.setVisible(false);
					}

					pause = !pause;
					break;

				case SPACE:
					// "Special word" to end the game
					if (Scenes.input.getText().equals("killmenow")) curtain(scene, root);
					// List for words to be deleted from "words" list
					List<Word> del = new ArrayList<Word>();
					// List for words to be added to "words" list
					List<Word> add = new ArrayList<Word>();

					for (Word w : words)
					{
						// If typed word is equal to any currently displayed
						if (w.getValue().equals(Scenes.input.getText()))
						{
							// For marathon add time for typed word
							timeLeft += (typedWords > 0) ? w.getLength() / 3 * multiplier : 0;
							// Add points accordingly to multiplier
							points += w.getLength() * multiplier;
							// Increase multiplier
							multiplier += multiplierAdd;
							typedWords++;
							// Increase the amount of typed words and characters
							typedChars += w.getValue().length();
							fresh.remove(w);
							// Remove from new words and add to deletion from main list
							del.add(w);
							// Remove from pane
							root.getChildren().remove(w);
							// Update the points
							Scenes.pointsVal.setText(String.valueOf(Math.round(points)));

							switch (typedWords)
							{
								case 1:
									startTime = System.nanoTime();
									// After typing first word start timer for CPM
									game_timer.start();

									// Add [m] new words
									for (int i = 0; i < howMany; i++)
									{
										Word word = createWord(strings, xVal_final, yVal_final, fresh);
										fresh.add(word);
										add.add(word);
										root.getChildren().add(word);
									}
									break;

								case 3:
									for (int i = 0; i < 3; i++)
									{
										Word word = createWord(strings, xVal_final, yVal_final, fresh);
										fresh.add(word);
										add.add(word);
										root.getChildren().add(word);
									}
									break;

								default:
									// Every 6th typed word add [m] new words
									if (typedWords % 6 == 0)
										for (int i = 0; i < howMany; i++)
										{
											Word word = createWord(strings, xVal_final, yVal_final, fresh);
											fresh.add(word);
											add.add(word);
											root.getChildren().add(word);
										}
									break;
							}
						}
					}
					// Add words
					words.addAll(add);
					// Delete words
					words.removeAll(del);
					// Clear text field
					Scenes.input.clear();
					break;
				default:
					break;
			}
		});
	}
}

