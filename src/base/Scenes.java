package base;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import base.obj.ScoreboardEntry;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import menu.Select;
import menu.Words;
import menu.obj.Option;
import menu.obj.ScaleBox;

import static base.Utils.showScoreboardPage;
import static base.Utils.blinkingNodeTimer;

public class Scenes {

	private final static String FONT_TITLE = "Grixel Kyrou 7 Wide Bold";
	final static String FONT_TEXT = "Courier new";
	
	static Text pointsVal = new Text("0");
	static Text conditionVal = new Text();	// create empty text and assign value later based on gamemode
	static Text CPM = new Text("0");
	static final StackPane pauseBox = new StackPane();

	public static final TextField input = new TextField();
	public static final Text pointer = createText(">", Color.WHITE, FONT_TEXT, 15);
	public static final Option[] gamemodes = {
			new Option(250, "Normal", 40, true),
			new Option(290, "Marathon", 40, false)
	};
	
	public static ScaleBox[][] scales;
	public static Option[] difficulties = new Option[5];
	public static String[] loadedDifficulties = Words.loadDifficulties();
	public static Option[] lngs;
	public static String fontsPath;
	
	private static int currentPage = 1;
	private static boolean saved;
	
	public static Scene scoreboard() {
		
		boolean newScore = false;
		saved = false;
				
		Pane root = new Pane();
			root.setPrefSize(800, 500);
			root.setStyle("-fx-background-color: rgb(14, 14, 14)");
		
		/* text with error information when SQLException occurs */
		Text errorMsg = createText("Error getting some scores", Colors.COLOR_RED_C, FONT_TEXT, 15);
			errorMsg.setTranslateX(10);
			errorMsg.setTranslateY(20);
			errorMsg.setVisible(false);
				
		Text title = createText("SCOREBOARD", Color.WHITE, FONT_TITLE, 26);
			title.setTranslateY(57);
			title.setTranslateX(257);
			
		StackPane nameInput = Utils.inputBox("NAME");
				nameInput.setTranslateX((800 - nameInput.getPrefWidth())/2);
				nameInput.setTranslateY((500 - nameInput.getPrefHeight())/2);
				nameInput.setVisible(false);	
			
		Pane resultsContainer = new Pane();
			resultsContainer.setTranslateX(25);
			resultsContainer.setTranslateY(100);
			resultsContainer.setPrefSize(752, 354);
			resultsContainer.setStyle("-fx-border-color: white;");
			
		Text newScoreText = createText("New score - Press enter to save", Colors.COLOR_GREEN_C, FONT_TEXT, 15);
			newScoreText.setTranslateY(85);
			newScoreText.setTranslateX(261);
		AnimationTimer newScoreTimer = blinkingNodeTimer(newScoreText, 750_000_000);
			newScoreTimer.start();
			
		StackPane pageStack = new StackPane();
			pageStack.setTranslateY(465);
			pageStack.setTranslateX(325);
			pageStack.setPrefSize(150, 25);
			
		Text pagePrev = createText("<", Color.WHITE, FONT_TEXT, 16);
			pagePrev.setTranslateX(-30);
			pagePrev.setVisible(false);
			
		Text pageNext = createText(">", Color.WHITE, FONT_TEXT, 16);
			pageNext.setTranslateX(30);
			
		Text pageNumber = createText("1", Color.WHITE, FONT_TEXT, 16);

		pageStack.getChildren().addAll(pagePrev, pageNumber, pageNext);	
			
		/* animation timer with floating particles */
		AnimationTimer animation_bg = Utils.getBackgroundTimer(790, 590, root, resultsContainer, newScoreText, nameInput);
			animation_bg.start();
			
		/* create 1st entry as table headers */
		final ScoreboardEntry headers = new ScoreboardEntry(1, "SCORE", "CPM", "DIFFICULTY", "LANGUAGE", "GAMEMODE", "DATE", "NAME");
		resultsContainer.getChildren().add(headers);
	
		final ArrayList<ScoreboardEntry> entries = new ArrayList<>();	// list of all returned scores
		try {
			final ResultSet results = Utils.getScores();

			int i = 0;
			while(results.next()) {
				boolean active = false;
				final String score = String.valueOf(results.getInt("Score"));
				final String cpm = String.valueOf(results.getInt("AvgCPM"));
				final String diff = results.getString("Difficulty");
				final String lang = results.getString("Language");
				final String gm = results.getString("Gamemode");
				final String date = Utils.formatDate(Long.valueOf(results.getString("DatePlayed")), "dd.MM.yy HH:mm");
				final String username = results.getString("Name");
					if(username.equals("NULL")) {
						active = true;
						newScore = true;
					}
					
				ScoreboardEntry e = new ScoreboardEntry(23+(i%15)*22, ++i, active, score, cpm, diff, lang, gm, date, active ? "" : username);
					e.setDate(results.getString("DatePlayed"));
				entries.add(e);
			}
		} catch (SQLException e) {
			Log.error("Could not retrieve scoreboard information: " + e);
			errorMsg.setVisible(true);	// show error message
		}
		
		final int pages = (int) Math.ceil(entries.size()/15.0);

		pageNext.setVisible(pages > 1);
		newScoreText.setVisible(newScore);
		title.setTranslateY(newScore ? 57 : 62);
		
		showScoreboardPage(currentPage, resultsContainer, entries, pageNumber);
		
		root.getChildren().addAll(resultsContainer, title, errorMsg, pageStack, newScoreText, nameInput);
		root.setId(String.valueOf(newScore));

		Scene scene = new Scene(root);
		
		final ScoreboardEntry activeEntry = ScoreboardEntry.activeEntry;
		scene.setOnKeyPressed(e -> {
			
			switch(e.getCode()) {
				case RIGHT:
					if(currentPage + 1 <= pages) {
						showScoreboardPage(++currentPage, resultsContainer, entries, pageNumber);
					}
				break;
				case LEFT:
					if(currentPage - 1 >= 1) {
						showScoreboardPage(--currentPage, resultsContainer, entries, pageNumber);
						
					}
				break;
				case ESCAPE:
					if(!saved) Utils.removeCurrentScore(activeEntry.getDate());
					Select.selectGamemode();
					break;
				case ENTER:
					if(Boolean.valueOf(root.getId())){
						newScoreTimer.stop();
						newScoreText.setVisible(false);
						title.setTranslateY(62);
						
						nameInput.setVisible(true);
						root.setId("false");
					} else {
						final TextField input = (TextField) nameInput.getChildren().get(nameInput.getChildren().size()-1);

						if(nameInput.isVisible() && input.getText().trim().length() > 0) {
							/* because gameOver() is called whenever the window is closed, check if there is anything to save, else return */
							final String name = input.getText();
							if(Utils.setScoreName(name)) {
								activeEntry.setName(name);
								ScoreboardEntry.activeEntry = null;
								saved = true;
							} else {
								errorMsg.setText("Could not save name");
								errorMsg.setVisible(true);
							}
							nameInput.setVisible(false);
						}
					}
						
					
				default: break;
			}
			pagePrev.setVisible(currentPage != 1);
			pageNext.setVisible(currentPage != pages);
		});
		return scene;
	}
			
	public static Pane selectMenu(String type) {
		Pane root = new Pane();
			root.setPrefSize(800, 500);
			root.setStyle("-fx-background-color: rgb(14, 14, 14)");
	
		Text header = createText(type, Color.WHITE, FONT_TITLE, 50);
			header.setTranslateX((800 - header.getLayoutBounds().getWidth())/2);
			header.setTranslateY(130);
		
		root.getChildren().add(header);
		
		switch(type) {
			case "GAMEMODE":
				root.getChildren().addAll(gamemodes);
				break;
				
			case "DIFFICULTY":
				for(int i=0; i<5; i++) {
					difficulties[i] = new Option((i==4) ? 345 : 220 + 25*i, loadedDifficulties[i], i==0);	// list all difficulties with one empty line for 'Custom'
				}
				root.getChildren().addAll(difficulties);
				break;
				
			case "CUSTOM":
				Text subHeader = createText("DIFFICULTY", Color.WHITE,FONT_TITLE, 18);
					subHeader.setTranslateX((800 - subHeader.getLayoutBounds().getWidth())/2);
					subHeader.setTranslateY(120 + subHeader.getLayoutBounds().getHeight());
					
				int startY = Window.gameMode == 0 ? 260 : 245;
				StackPane sPaneText = new StackPane();
					sPaneText.setTranslateX(245);
					sPaneText.setTranslateY(startY);
					sPaneText.setAlignment(Pos.CENTER_LEFT);
					
				StackPane sPaneScales = new StackPane();
					sPaneScales.setTranslateX(385);
					sPaneScales.setTranslateY(startY);
					sPaneScales.setAlignment(Pos.CENTER_LEFT);
					
				pointer.setTranslateX(-30);
				
				Text howFast = createText("Speed", Color.WHITE, FONT_TEXT, 16);
				
				Text howOften = createText("Frequency", Color.WHITE, FONT_TEXT, 16);
					howOften.setTranslateY(30);

				Text howMany = createText("Amount", Color.WHITE, FONT_TEXT, 16);
					howMany.setTranslateY(60);
					
				Text startTime = createText(Window.gameMode == 0 ? "" : "Start time", Color.WHITE, FONT_TEXT, 16);	
					startTime.setTranslateY(90);
										
				scales = new ScaleBox[Window.gameMode == 0 ? 3 : 4][10];
				for(int i=0; i<scales.length; i++) {
					for(int j=0; j<10; j++) {
						int x = j*17; int y = i*30;
						scales[i][j] = new ScaleBox(x, y);	// 
						sPaneScales.getChildren().add(scales[i][j]);
					}
				}
				
				
				sPaneText.getChildren().addAll(pointer, howFast, howOften, howMany, startTime);
				root.getChildren().addAll(subHeader, sPaneText, sPaneScales);
				break;
				
			case "LANGUAGES":
				String[][] loadedLanguages = Words.loadLanguages();
				lngs = new Option[Words.how_many_lngs];	// needs to be defined here because before 'Words.loadLanguages()' is called 'how_many_lngs' will be 0
				
				/* load all available languages */
				for(int i=0; i<Words.how_many_lngs; i++) {
					lngs[i] = new Option(220 + 25*i, loadedLanguages[i][0], loadedLanguages[i][1], i==0);
				}
				root.getChildren().addAll(lngs);
				break;
		}
		return root;	// return pane with selected elements
	}
	
	public static Pane gameOver() {
		
		Pane root = new Pane();
			root.setPrefSize(800, 500);
			root.setStyle("-fx-background: " + Colors.BACKGROUND);
			
		StackPane stack = new StackPane();
			stack.setTranslateY(200);
			
		Rectangle background = new Rectangle(800, 500);
			background.setTranslateX(0);
			background.setTranslateY(0);
			background.setFill(Colors.BACKGROUND_C);
				
		int pointsLen = String.valueOf(Math.round(Window.points)).length();		// calculation needed for good placement of the points
		Text pointsText = new Text("Your score: ");
			pointsText.setFont(Font.font(FONT_TITLE, 30));
			pointsText.setFill(Color.WHITE);
			pointsText.setTranslateX(400-(300+pointsLen*36)/2);
			
		Text pointsVal = new Text(Scenes.pointsVal.getText());
			pointsVal.setFont(Font.font(FONT_TITLE, 32));
			pointsVal.setFill(Color.web("#FF554D"));
			pointsVal.setTranslateX(pointsText.getTranslateX() + (305+pointsLen*36)/2);
		
		stack.getChildren().addAll(pointsText, pointsVal);
		root.getChildren().addAll(background, stack);
					
		return root;
	}
	
	public static Pane game() {
		
		Pane root = new Pane();
			root.setPrefSize(800, 500);
				
		Rectangle bottomLine = new Rectangle(800, 5);
			bottomLine.setTranslateX(0); bottomLine.setTranslateY(400);
			bottomLine.setFill(Color.web("#131313"));
		
		Rectangle background = new Rectangle(800, 500);
			background.setTranslateX(0); background.setTranslateY(0);
			background.setFill(Colors.BACKGROUND_C);	
			
		int pointsLen = String.valueOf(Math.round(Window.points)).length()+5;
				
		Text pointsText = new Text("Points: ");
			pointsText.setFill(Color.WHITE);
			pointsText.setTranslateX(30);
			pointsText.setFont(Font.font(FONT_TEXT, 17));
			
			pointsVal.setFill(Colors.COLOR_GREEN_C);
			pointsVal.setTranslateX(50+10*pointsLen);
			pointsVal.setFont(Font.font(FONT_TEXT, 17));
		
		Text conditionText = new Text(Window.gameMode == 0 ? "Missed: " : "Time left: ");	// if default gamemode, set "Missed" else "Time left" for marathon mode
			conditionText.setFill(Color.WHITE);
			conditionText.setTranslateX(230);
			conditionText.setFont(Font.font(FONT_TEXT, 17));
			
			conditionVal.setFill(Colors.COLOR_RED_C);
			conditionVal.setTranslateX(conditionText.getTranslateX() + conditionText.getLayoutBounds().getWidth());
			conditionVal.setFont(Font.font(FONT_TEXT, 17));
		
		Text CPMText = new Text("CPM: ");
			CPMText.setFill(Color.WHITE);
			CPMText.setTranslateX(230);
			CPMText.setFont(Font.font(FONT_TEXT, 17));
			
			CPM.setFill(Colors.COLOR_YELLOW_C);
			CPM.setTranslateX(280);
			CPM.setFont(Font.font(FONT_TEXT, 17));
			
		Text signL = new Text("[");
			signL.setFill(Color.WHITE);
			signL.setTranslateX(27);
			signL.setFont(Font.font(FONT_TEXT, 17));
		
		Text signR = new Text("]");
			signR.setFill(Color.WHITE);
			signR.setTranslateX(27+120+5);
			signR.setFont(Font.font(FONT_TEXT, 17));
			
		Text pause = new Text("PAUSE");
			pause.setFill(Colors.COLOR_RED_C);
			pause.setFont(Font.font(FONT_TEXT, 25));
			
		Rectangle pauseBg = new Rectangle(100, 40);
			pauseBg.setFill(Colors.BACKGROUND_C);
		
		pauseBox.getChildren().addAll(pauseBg, pause);
			pauseBox.setTranslateX(350);
			pauseBox.setTranslateY(200);
			pauseBox.setVisible(false);
			
		input.setPrefWidth(120);
		input.setPrefHeight(20); 
		input.setMaxWidth(120);
		input.setMaxHeight(20);
		input.setTranslateX(35);
		input.setStyle("-fx-faint-focus-color: transparent;"
				+ "-fx-focus-color: transparent;"
				+ "-fx-text-box-border: transparent;"
				+ "-fx-background-color: #0e0e0e;"
				+ "-fx-text-fill: #FFF;"
				+ "-fx-highlight-fill: #FFF;"
				+ "-fx-highlight-text-fill: #000;"
				+ "-fx-cursor: block;"
				+ "-fx-font-family: 'Courier new', monospace;");
		
		StackPane topStack = new StackPane();
			topStack.setTranslateX(0);
			topStack.setTranslateY(425);
			topStack.setMaxWidth(800);
			topStack.setPrefWidth(800);
		topStack.getChildren().addAll(pointsText, pointsVal, conditionText, conditionVal);
				
		StackPane bottomStack = new StackPane();
			bottomStack.setTranslateX(0);
			bottomStack.setTranslateY(455);
			bottomStack.setMaxWidth(800);
			bottomStack.setPrefWidth(800);
		bottomStack.getChildren().addAll(input, signL, signR, CPMText, CPM);
		
		bottomStack.setAlignment(Pos.CENTER_LEFT);
		topStack.setAlignment(Pos.CENTER_LEFT);
				
		root.getChildren().addAll(background, bottomLine, topStack, bottomStack, pauseBox);
	
		return root;
	}
		
	public static StackPane error(String err) {
		
		StackPane root = new StackPane();
			root.setPrefSize(800, 500);
			root.setStyle("-fx-background-color: rgb(14, 14, 14)");
					
		Rectangle errorBox = new Rectangle(350, 130);
			errorBox.setFill(Color.web("#0e0e0e"));		
			errorBox.setStyle("-fx-stroke: white; fx-stroke-width: 2");
		
		Label error = new Label(" ERROR ");
			error.setTranslateY(-65);
			error.setStyle("-fx-background-color: #0e0e0e;");
			error.setTextFill(Color.WHITE);	
			error.setFont(Font.font(FONT_TEXT, 25));
			
		Text errorMsg = new Text();
			errorMsg.setWrappingWidth(350);
			errorMsg.setFill(Color.WHITE);
			errorMsg.setFont(Font.font(FONT_TEXT, 20));
			errorMsg.setTextAlignment(TextAlignment.CENTER);

		switch(err) {
			case "MISSING_WORDS": 
				errorMsg.setText("Missing folder with words");
			break;
			
			case "TEST":
				errorMsg.setText("LONG TEST TEXT TO SEE HOW IT WRAPS");
			break;
			
		}
		
		root.getChildren().addAll(errorBox, errorMsg, error);
		
		return root;
	}
	
	public static Text createText(String value, Color fill, String fontName, int fontSize) {
		Text t = new Text(value);
		t.setFont(Font.font(fontName, fontSize));
		t.setFill(fill);
		
		return t;
	}
	
	public static void fontSetup() {
		/* list of required font names */
		String[] fontNames = {
				"Grixel Kyrou 7 Wide Bold.ttf",
				"Courier New.ttf",
				"Courier New Bold.ttf"
		};
		
		/* load each font */
		for(String font : fontNames) {
			try {
				Font.loadFont(Scenes.class.getResourceAsStream("/resources/fonts/" + font), 20);
			} catch (Exception e) {
				Log.error(String.format("Unable to load font {%s}: {%s}", font, e));
			}
		}		
	}
	
	/* spaghetti for downloading and loading fonts */
//	public static void fontSetup() {
//		
//		/* fonts to be loaded */
//		String[] fontNames = {	"Grixel Kyrou 7 Wide Bold.ttf",
//								"Courier New.ttf",
//								"Courier New Bold.ttf" };	
//				
//		fontsPath = Window.saveDirectory;
//		
//		if(!new File(fontsPath).exists() && !new File(fontsPath).mkdir()) {		// if 'imspeed' folder doesn't exist and cannot be created throw an error
//			System.err.println("[ERROR] Could not create 'imspeed' directory");
//		} else {
//			fontsPath += "fonts" + Window.slash;	// add 'fonts' to the path with OS-dependent slash
//			
//			if(!new File(fontsPath).exists() && !new File(fontsPath).mkdir()) {		// if 'fonts' folder doesn't exist and cannot be created throw an error
//				System.err.println("[ERROR] Could not create 'fonts' directory");
//			} else {
//				
//				for(int i=0; i<fontNames.length; i++) {		// iterate all fonts
//					if(!new File(fontsPath + fontNames[i]).exists()) {
//						try {
//							URL font = new URL("https://kazmierczyk.me/--imspeed/" + URLEncoder.encode(fontNames[i], "UTF-8").replace("+", "%20"));		// change encoding for url
//							InputStream url = font.openStream();
//							Files.copy(url, Paths.get(fontsPath + fontNames[i]));	// download fonts from private hosting and save in dedicated folder
//							url.close();
//							System.out.println("[OK] Success downloading {" + fontNames[i] + "}");
//						} catch (Exception e) {
//							System.err.println("[ERROR] Could not download font: " + e);
//						}		
//					} else {
//						System.out.println("[OK] {" + fontNames[i] + "} already downloaded");
//					}
//				}
//			}
//		}
//		
//		/* load every front */
//		for(int i=0; i<fontNames.length; i++) {
//			try {
//				InputStream font = new FileInputStream(fontsPath + fontNames[i]);
//				Font.loadFont(font, 15);
//			} catch (FileNotFoundException e) {
//				System.err.println("[Error] Could not load font file: " + e);
//			}
//		} System.out.println();
//	}
	
}