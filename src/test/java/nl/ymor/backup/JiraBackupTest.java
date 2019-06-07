package nl.ymor.backup;

import nl.ymor.config.TestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@TestPropertySource(locations = "classpath:application.properties")
public class JiraBackupTest {

    @Test
    public void testRun() {
        //TODO write
        assertThat(true).isTrue();
    }
}
