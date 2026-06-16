package com.adaptive.server.responses;

public class BonusQuestionResponse extends QuestionResponse {

    public static final int BONUS_STARS = 50; //זה 50 כוכבים לאחר שתלמיד ענה על שאלת בונוס קשה

    public BonusQuestionResponse() {
        super();
        setBonus(true);
        setStarsReward(BONUS_STARS);
    }
}
