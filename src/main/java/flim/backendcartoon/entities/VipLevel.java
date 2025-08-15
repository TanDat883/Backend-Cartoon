/*
 * @(#) $(NAME).java    1.0     7/11/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;


/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 11-July-2025 7:22 PM
 */
public enum VipLevel {
    FREE("Free", 0),
    NO_ADS("NoAds", 1),
    PREMIUM("Premium", 2),
    K_PLUS("KPlus", 3),
    COMBO_PREMIUM_K_PLUS("ComboPremiumKPlus", 4);

    private final String levelName;
    private final int levelValue;

    VipLevel(String levelName, int levelValue) {
        this.levelName = levelName;
        this.levelValue = levelValue;
    }

    public String getLevelName() {
        return levelName;
    }

    public int getLevelValue() {
        return levelValue;
    }
}
