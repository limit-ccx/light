package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequestDto {

    private String identifier;

    /**
     * 兼容旧客户端，新客户端优先使用 identifier。
     */
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(max = 72, message = "密码长度不能超过72个字符")
    private String password;

    private boolean remember = true;
}
