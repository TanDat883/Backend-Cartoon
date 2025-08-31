/*
 * @(#) $(NAME).java    1.0     8/28/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;


/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 28-August-2025 8:35 AM
 */
public enum DiscountType {
    PERCENTAGE("Percentage"),
    FIXED_AMOUNT("Fixed Amount");

    private final String type;

    DiscountType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
