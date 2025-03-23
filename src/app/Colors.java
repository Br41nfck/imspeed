package app;

import javafx.scene.paint.Color;

/*
Constants for all used colors
_C indicates a Color object
*/

public interface Colors {
	public static final String BACKGROUND = "#0E0E0E";
	public static final String RED = "#FF554D";
	public static final String ORANGE = "#FCA103";
	public static final String YELLOW = "#F0FC03";
	public static final String GREEN = "#7FFC03";
	public static final String LIGHT_GREY = "#515658";
	public static final String MID_GREY = "#2E2E2E";
	public static final String DARK_GREY = "#1A1A1A";
	public static final String GOLD_GRADIENT = "-fx-fill: linear-gradient(#FFA200, #FFD500);";

	public static final Color BACKGROUND_C = Color.web(BACKGROUND);
	public static final Color RED_C = Color.web(RED);
	public static final Color ORANGE_C = Color.web(ORANGE);
	public static final Color YELLOW_C = Color.web(YELLOW);
	public static final Color GREEN_C = Color.web(GREEN);
	public static final Color LIGHT_GREY_C = Color.web(MID_GREY);
	public static final Color DARK_GREY_C = Color.web(DARK_GREY);
}