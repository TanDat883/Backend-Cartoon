package flim.backendcartoon.entities;


import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.LocalDate;
import java.util.List;

@DynamoDbBean
public class User {

    private String userId;
    private String phoneNumber;
    private String userName;
    private String dob;
    private Role role;
    private String email;
    private String avatarUrl;
    private VipLevel vipLevel;
    private LocalDate vipStartDate;
    private LocalDate vipEndDate;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DynamoDbAttribute("dob")
    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    @DynamoDbAttribute("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @DynamoDbAttribute("userName")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @DynamoDbAttribute("role")
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @DynamoDbAttribute("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("avatarUrl")
    public String getAvatarUrl() {return avatarUrl;}

    public void setAvatarUrl(String avatarUrl) {this.avatarUrl = avatarUrl;}

    @DynamoDbAttribute("vipLevel")
    public VipLevel getVipLevel() {return vipLevel; }

    public void setVipLevel(VipLevel vipLevel) { this.vipLevel = vipLevel;}

    @DynamoDbAttribute("vipStartDate")
    public LocalDate getVipStartDate() {
        return vipStartDate;
    }

    public void setVipStartDate(LocalDate vipStartDate) {
        this.vipStartDate = vipStartDate;
    }

    @DynamoDbAttribute("vipEndDate")
    public LocalDate getVipEndDate() {
        return vipEndDate;
    }

    public void setVipEndDate(LocalDate vipEndDate) {
        this.vipEndDate = vipEndDate;
    }

}
