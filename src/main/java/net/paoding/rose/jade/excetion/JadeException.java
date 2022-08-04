package net.paoding.rose.jade.excetion;

import org.slf4j.helpers.MessageFormatter;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2020-04-22 11:54:00
 */
public class JadeException extends RuntimeException {

    private String message;

    public JadeException(String message, Object... objects) {
        this.message = MessageFormatter.arrayFormat(message, objects).getMessage();
    }

    public JadeException(String message) {
        this.message = message;
    }

    public JadeException(String message, Throwable throwable) {
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
