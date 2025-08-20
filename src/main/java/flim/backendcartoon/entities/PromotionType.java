/*
 * @(#) $(NAME).java    1.0     8/17/2025
 *
 * Copyright (c) 2025 IUH. All rights reserved.
 */

package flim.backendcartoon.entities;


/*
 * @description
 * @author: Tran Tan Dat
 * @version: 1.0
 * @created: 17-August-2025 6:53 PM
 */
public enum PromotionType {
    VOUCHER("VOUCHER"), PACKAGE("PACKAGE");

    private final String type;

    PromotionType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
    }

}
