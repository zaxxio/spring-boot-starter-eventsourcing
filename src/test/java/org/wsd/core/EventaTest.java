package org.wsd.core;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.eventa.core.config.EventaAutoConfiguration;

@SpringBootTest
@ContextConfiguration(classes = EventaAutoConfiguration.class)
public class EventaTest {

    @Test
    public void eventaTest() {

    }

}
