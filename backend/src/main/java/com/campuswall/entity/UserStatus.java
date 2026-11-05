package com.campuswall.entity;

// 用户账号状态：正常、封禁、已注销，注销后学号会被清除，不再能登录。
public enum UserStatus {
    ACTIVE,
    BANNED,
    DELETED
}
