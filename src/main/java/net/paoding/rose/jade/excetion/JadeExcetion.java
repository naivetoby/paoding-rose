package net.paoding.rose.jade.excetion;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2020-04-22 11:54:00
 */
public class JadeExcetion extends RuntimeException {

    private String message;

    public JadeExcetion(String message, Object... objects) {
        if (message != null && objects.length > 0 && message.contains("{}")) {
            for (Object obj : objects) {
                message = message.replaceFirst("\\{}", obj == null ? "null" : String.valueOf(obj));
            }
        }
        this.message = message;
    }

    public JadeExcetion(String message) {
        this.message = message;
    }

    public JadeExcetion(String message, Throwable throwable) {
        this.message = message;
        super.addSuppressed(throwable);

    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
