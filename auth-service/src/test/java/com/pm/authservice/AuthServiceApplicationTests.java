package com.pm.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "jwt.secret=dGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLWNvbnRleHQtdGVzdHM=")
class AuthServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
