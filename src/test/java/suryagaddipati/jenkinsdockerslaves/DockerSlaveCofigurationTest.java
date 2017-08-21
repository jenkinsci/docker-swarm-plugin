package suryagaddipati.jenkinsdockerslaves;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class DockerSlaveCofigurationTest {
    @Test
    public  void testLoadFromYaml() throws IOException {
        final InputStream stream = getClass().getResourceAsStream("/configuration.yml");
        final String payloadReq = IOUtils.toString(stream);
        System.out.println(payloadReq);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//        DockerSlaveConfiguration config = mapper.readValue(payloadReq, DockerSlaveConfigurationLoader.class);
//        System.out.println(config.getLabelConfigurations().get(0).getImage());
    }


}
