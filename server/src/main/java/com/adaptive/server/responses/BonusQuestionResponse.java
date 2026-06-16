package com.adaptive.server.responses;

public class BonusQuestionResponse extends QuestionResponse {

    /** Stars awarded to the user when this bonus question is answered correctly. */
    public static final int BONUS_STARS = 50;

    public BonusQuestionResponse() {
        super();
        setBonus(true);
        setStarsReward(BONUS_STARS);
    }
}
