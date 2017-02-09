package com.scienjus.smartqq;

import com.scienjus.smartqq.callback.MessageCallback;
import com.scienjus.smartqq.client.SmartQQClient;
import com.scienjus.smartqq.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author ScienJus
 * @date 2015/12/18.
 */
public class Application {
    /**
     * 图灵机器人API KEY, 请见app.properties中tulingkey
     */
    private static String TU_LING_API_KEY;

    /**
     * 监听收到好友的QQ消息的QQ号, 请见app.qqnumbers
     */
    private static Long[] LISTEN_QQ_NUMBERS;

    /**
     * 监听收到群消息的群名称, 请见app.properties中groupnames
     */
    private static String[] LISTEN_QQ_GROUPS;

    public static void main(String[] args) {
        // 间歇加载要监听的QQ号码和群名称
        Timer loadNumbersTimer = new Timer();
        loadNumbersTimer.schedule(new LoadNumbersTimerTask(), 100, 1000 * 60 * 5);

        // 创建一个新对象时需要扫描二维码登录，并且传一个处理接收到消息的回调，如果你不需要接收消息，可以传null
        SmartQQClient client = new SmartQQClient(new MessageCallback() {
            @Override
            public void onMessage(SmartQQClient client, Message message) {
                long qqNumber = client.getQQById(message);
                long userId = message.getUserId();
                UserInfo friendInfo = client.getFriendInfo(userId);
                String content = message.getContent();
                String nickName = friendInfo.getNick();
                System.out.println("QQ消息(" + nickName + " [" + qqNumber + "]): " + content);
                if (Arrays.asList(LISTEN_QQ_NUMBERS).contains(qqNumber)) {
                    String reply = tulingMsg(content);
                    System.out.println("回复QQ消息(" + nickName + " [" + qqNumber + "]): " + reply);
                    client.sendMessageToFriend(userId, reply);
                }
            }

            @Override
            public void onGroupMessage(SmartQQClient client, GroupMessage message) {
                String content = message.getContent();
                GroupInfo groupInfo = client.getGroupInfo(message.getGroupId());
                String groupName = groupInfo.getName();
                System.out.println("群消息(" + groupName + "): " + content);
                if (Arrays.asList(LISTEN_QQ_GROUPS).contains(groupName)) {
                    String reply = tulingMsg(content);
                    System.out.println("回复群消息(" + groupName + "): " + reply);
                    client.sendMessageToGroup(message.getGroupId(), reply);
                }

            }

            @Override
            public void onDiscussMessage(SmartQQClient client, DiscussMessage message) {
                DiscussInfo discussInfo = client.getDiscussInfo(message.getDiscussId());
                System.out.println("讨论组消息(" + discussInfo.getName() + "): " + message.getContent());
            }
        });
        // 登录成功后便可以编写你自己的业务逻辑了
        List<Category> categories = client.getFriendListWithCategory();
        for (Category category : categories) {
            System.out.println(category.getName());
            for (Friend friend : category.getFriends()) {
                System.out.println("————" + friend.getNickname());
            }
        }

        // 阻塞
        while (true) ;
        // 使用后调用close方法关闭，你也可以使用try-with-resource创建该对象并自动关闭
//        try {
//            client.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * 从图灵机器人获取回复
     *
     * @param talkMsg
     * @return
     */
    private static String tulingMsg(String talkMsg) {
        String data = null;
        try {
            data = URLEncoder.encode(talkMsg, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String strUrl = "http://www.tuling123.com/openapi/api?key=" + TU_LING_API_KEY + "&info=+" + data;
        URL url = null;
        try {
            url = new URL(strUrl);
            HttpURLConnection conn = null;
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko");
            conn.connect();
            //打开这个页面的输入流，这个网站的内容以字节流的形式返回。如果是网页就返回html，图片就返回图片的内容。
            InputStream inStream = conn.getInputStream();
            byte[] buf = new byte[1024];
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            int n = 0;
            while ((n = inStream.read(buf)) != -1) {
                outStream.write(buf, 0, n);
            }
            inStream.close();
            outStream.close();
            //用ByteArrayOutputStream全部缓冲好后再一次转成String，不然再间隔的地方会出现乱码问题
            String result = outStream.toString();
            //返回的JSON，弄成字符串后去掉头和尾就行
            result = result.substring(23, result.length() - 2);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 定时加载app.properties
     */
    static class LoadNumbersTimerTask extends TimerTask {
        Properties properties = new Properties();

        @Override
        public void run() {
            try {
                String now = new SimpleDateFormat("HH:mm").format(new Date());
                System.out.println("[" + now + "] 加载监听的QQ号和群名称: ");
                properties.load(this.getClass().getClassLoader().getResourceAsStream("app.properties"));
                TU_LING_API_KEY = properties.getProperty("tulingkey", "");
                String qqStrNumbers = properties.getProperty("qqnumbers");
                String groupStrNames = properties.getProperty("groupnames");
                if (qqStrNumbers.length() > 0) {
                    String[] split = qqStrNumbers.split(",");
                    Long[] qqNumbers = new Long[split.length];
                    long[] longs = Arrays.stream(split).distinct().map(String::trim).mapToLong(Long::valueOf).toArray();
                    for (int i = 0; i < split.length; i++) {
                        qqNumbers[i] = longs[i];
                    }
                    LISTEN_QQ_NUMBERS = qqNumbers;
                    System.out.println("--正在监听的QQ号: " + qqStrNumbers);
                } else {
                    LISTEN_QQ_NUMBERS = new Long[]{};
                }

                if (groupStrNames.length() > 0) {
                    String[] splitStrGroupNames = groupStrNames.split(",");
                    String[] groupNames = new String[splitStrGroupNames.length];
                    for (int j = 0; j < splitStrGroupNames.length; j++) {
                        groupNames[j] = splitStrGroupNames[j].trim();
                    }
                    LISTEN_QQ_GROUPS = groupNames;
                    System.out.println("--正在监听的QQ群: " + groupStrNames);
                } else {
                    LISTEN_QQ_GROUPS = new String[]{};
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
