package com.project.pms.entity.po;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @className: Command
 * @description: 命令
 * @author: loser
 * @createTime: 2026/2/10 16:30
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Command {
    private Integer code ;
    private String content;
}
