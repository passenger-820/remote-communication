package cn.edu.cqu.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @param <T>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObjectWrapper<T>  {
    private Byte code;
    private String type;
    private T impl;
}
