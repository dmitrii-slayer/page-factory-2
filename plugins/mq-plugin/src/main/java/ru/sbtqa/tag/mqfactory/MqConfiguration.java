package ru.sbtqa.tag.mqfactory;

import org.aeonbits.owner.Config;
import ru.sbtqa.tag.pagefactory.properties.Configuration;

public interface MqConfiguration extends Config {

    @Key("mq.timeout")
    @DefaultValue("50000")
    Long getMqTimeout();

    @Key("kafka.timeout")
    @DefaultValue("10000")
    Long getKafkaTimeout();

    static MqConfiguration create() {
        return Configuration.init(MqConfiguration.class);
    }
}
