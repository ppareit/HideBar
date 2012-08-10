package be.ppareit.hidebar;

public class Constants {

    /** Is the application build as demo or for paid users */
    enum MarketType {
        DEMO, PAID;
    }

    public static final MarketType MARKETTYPE = MarketType.DEMO;

    public static final String ACTION_BARHIDDEN = "be.ppareit.hidebar.ACTION_BARHIDDEN";
    public static final String ACTION_BARSHOWN = "be.ppareit.hidebar.ACTION_BARSHOWN";
}
