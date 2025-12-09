package lambda.fase4.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Configuração dos clientes AWS SDK.
 *
 * IMPORTANTE: Serviços em us-east-2, EXCETO SES em us-east-1
 *
 * Utiliza DefaultCredentialsProvider que busca credenciais em:
 * 1. IAM Role (quando rodando em Lambda) - RECOMENDADO
 * 2. Variáveis de ambiente (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * 3. Arquivo de credenciais (~/.aws/credentials)
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region:us-east-2}")
    private String awsRegion;

    @Value("${aws.ses.region:us-east-1}")
    private String sesRegion;

    @Bean
    @Primary
    public Region region() {
        return Region.of(awsRegion);
    }

    @Bean(name = "sesRegion")
    public Region sesRegionBean() {
        return Region.of(sesRegion);
    }

    /**
     * Cliente SQS em us-east-2
     */
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(region())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Cliente SNS em us-east-2
     */
    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(region())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Cliente CloudWatch em us-east-2
     */
    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(region())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Cliente SES FORÇADO para us-east-1
     * SES tem funcionalidades limitadas fora de us-east-1, us-west-2 e eu-west-1
     */
    @Bean
    public SesClient sesClient() {
        return SesClient.builder()
                .region(sesRegionBean())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}


