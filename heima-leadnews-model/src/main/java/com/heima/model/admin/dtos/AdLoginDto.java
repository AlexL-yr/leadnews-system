package com.heima.model.admin.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdLoginDto {


    @Schema(description = "用户名")
    private String name;

    @Schema(description = "密码")
    private String password;
}

