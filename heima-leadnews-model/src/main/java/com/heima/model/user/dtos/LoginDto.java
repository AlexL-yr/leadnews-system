package com.heima.model.user.dtos;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class LoginDto {

    /**
     * 手机号
     */
    @ApiModelProperty(value = "phone number",required = true)
    @Schema(description = "手机号", required = true)
    private String phone;

    /**
     * 密码
     */
    @ApiModelProperty(value = "password",required = true)
    @Schema(description = "密码", required = true)
    private String password;
}
