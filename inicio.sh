#!/bin/bash
mkdir -p src/main/java/com/rafaj2ee/model
mkdir -p src/main/resources/META-INF/spring
mkdir -p src/main/resources/db

# Criar arquivo de entidade Counter
cat > src/main/java/com/rafaj2ee/model/Counter.java <<EOF
package com.rafaj2ee.model;

import lombok.Data;

@Data
public class Counter {
    private Long id;
    private Integer count;
    private String name;
}
EOF

# Arquivo de configuração Spring Batch
cat > src/main/resources/META-INF/spring/spring-batch.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:batch="http://www.springframework.org/schema/batch"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans 
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/batch 
                           http://www.springframework.org/schema/batch/spring-batch.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd">

    <context:property-placeholder location="classpath:application.properties"/>

    <bean id="dataSource" class="org.springframework.jdbc.datasource.SimpleDriverDataSource">
        <property name="driverClass" value="\${spring.datasource.driver-class-name}"/>
        <property name="url" value="\${spring.datasource.url}"/>
    </bean>

    <bean id="transactionManager" 
          class="org.springframework.batch.support.transaction.ResourcelessTransactionManager"/>

    <bean id="jobRepository" 
          class="org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean">
        <property name="transactionManager" ref="transactionManager"/>
    </bean>

    <bean id="jobLauncher"
          class="org.springframework.batch.core.launch.support.SimpleJobLauncher">
        <property name="jobRepository" ref="jobRepository"/>
    </bean>

    <batch:job id="counterBackupJob">
        <batch:step id="backupStep">
            <batch:tasklet>
                <batch:chunk reader="counterReader" writer="counterWriter" commit-interval="10"/>
            </batch:tasklet>
        </batch:step>
    </batch:job>

    <bean id="counterReader" class="org.springframework.batch.item.database.JdbcCursorItemReader" scope="step">
        <property name="dataSource" ref="dataSource"/>
        <property name="sql" value="SELECT id, count, name FROM counter WHERE id = #{jobParameters['id']}"/>
        <property name="rowMapper">
            <bean class="org.springframework.jdbc.core.BeanPropertyRowMapper">
                <property name="mappedClass" value="com.rafaj2ee.model.Counter"/>
            </bean>
        </property>
    </bean>

    <bean id="counterWriter" class="org.springframework.batch.item.database.JdbcBatchItemWriter">
        <property name="dataSource" ref="dataSource"/>
        <property name="sql" value="INSERT INTO counter_backup (id, count, name) VALUES (:id, :count, :name)"/>
        <property name="itemSqlParameterSourceProvider">
            <bean class="org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider"/>
        </property>
    </bean>
</beans>
EOF

# Configurações do banco de dados
cat > src/main/resources/application.properties <<EOF
spring.datasource.url=jdbc:sqlite:counter.db
spring.datasource.driver-class-name=org.sqlite.JDBC
spring.datasource.username=
spring.datasource.password=
spring.batch.job.enabled=false
EOF

# Schema do banco
cat > src/main/resources/db/schema.sql <<EOF
CREATE TABLE IF NOT EXISTS counter (
    id INTEGER PRIMARY KEY,
    count INTEGER NOT NULL,
    name TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS counter_backup (
    id INTEGER PRIMARY KEY,
    count INTEGER NOT NULL,
    name TEXT NOT NULL
);
EOF

# Classe de aplicação
cat > src/main/java/com/rafaj2ee/BatchApplication.java <<EOF
package com.rafaj2ee;

import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BatchApplication {
    public static void main(String[] args) throws Exception {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "META-INF/spring/spring-batch.xml")) {
            
            JobLauncher jobLauncher = context.getBean(JobLauncher.class);
            Job job = context.getBean("counterBackupJob", Job.class);
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("id", args.length > 0 ? args[0] : "0")
                    .toJobParameters();
            
            JobExecution execution = jobLauncher.run(job, jobParameters);
            System.exit(SpringBatchExitCode.getExitCode(execution));
        }
    }
}

class SpringBatchExitCode {
    public static int getExitCode(JobExecution jobExecution) {
        return jobExecution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
    }
}
EOF

# Arquivo POM.xml
cat > pom.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.rafaj2ee</groupId>
    <artifactId>counter-backup</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>1.8</java.version>
        <spring.boot.version>2.7.18</spring.boot.version>
        <spring.batch.version>4.3.8</spring.batch.version>
        <sqlite.version>3.45.1.0</sqlite.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.batch</groupId>
            <artifactId>spring-batch-core</artifactId>
            <version>\${spring.batch.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-jdbc</artifactId>
            <version>5.3.30</version>
        </dependency>
        <dependency>
            <groupId>org.xerial</groupId>
            <artifactId>sqlite-jdbc</artifactId>
            <version>\${sqlite.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>\${java.version}</source>
                    <target>\${java.version}</target>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.1</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.rafaj2ee.BatchApplication</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
EOF

echo "Estrutura criada com sucesso!"
echo "Para construir e executar:"
echo "mvn clean package"
echo "java -jar target/counter-backup-1.0-SNAPSHOT.jar 123"