package cn.edu.cqu.utils;

import cn.edu.cqu.exceptions.NetworkException;
import lombok.extern.slf4j.Slf4j;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@Slf4j
public class NetUtils {
    /**
     * 获取局域网IP
     * @return 局域网IP
     */
    public static String getIP(){
        try {
            // 获取所有网卡信息
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()){
                NetworkInterface iface = interfaces.nextElement();
                //过滤非环接口和虚拟接口
                if (iface.isLoopback() || iface.isVirtual() || !iface.isUp()){
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()){
                    InetAddress addr = addresses.nextElement();
                    // 过滤IPv6地址和回环地址
                    if (addr instanceof Inet6Address || addr.isLoopbackAddress()){
                        continue;
                    }
                    String ipAddress = addr.getHostAddress();
                    if(log.isDebugEnabled()){
                        log.debug("局域网IP地址：{}",ipAddress);
                    }
                    return ipAddress;
                }
            }
            // 没有获取到，就抛出异常，处理得比较粗暴了
            throw new NetworkException();
        } catch (SocketException e) {
            log.error("Exception occurred while acquiring the LAN",e);
            throw new NetworkException(e);
        }
    }

    public static void main(String[] args) {
        final String ip = NetUtils.getIP();
        System.out.println("ip = " + ip);
    }
}
