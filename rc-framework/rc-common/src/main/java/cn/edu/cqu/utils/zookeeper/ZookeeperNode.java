package cn.edu.cqu.utils.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 节点实例
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZookeeperNode {
    private String nodePath;
    private byte[] data;
}
