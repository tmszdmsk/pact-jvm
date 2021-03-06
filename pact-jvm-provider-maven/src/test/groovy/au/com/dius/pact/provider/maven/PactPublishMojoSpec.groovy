package au.com.dius.pact.provider.maven

import au.com.dius.pact.provider.broker.PactBrokerClient
import org.apache.maven.plugin.MojoExecutionException
import spock.lang.Specification

import java.nio.file.Files

class PactPublishMojoSpec extends Specification {

  private PactPublishMojo mojo
  private PactBrokerClient brokerClient

  def setup() {
    brokerClient = Mock(PactBrokerClient)
    mojo = new PactPublishMojo(pactDirectory: 'some/dir', brokerClient: brokerClient, projectVersion: '0.0.0')
  }

  def 'uploads all pacts to the pact broker'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    3.times {
      def file = Files.createTempFile(dir, 'pactfile', '.json')
      file.write(pact)
    }
    mojo.pactDirectory = dir.toString()

    when:
    mojo.execute()

    then:
    3 * brokerClient.uploadPactFile(_, _, []) >> 'OK'

    cleanup:
    dir.deleteDir()
  }

  def 'Fails with an exception if any pacts fail to upload'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    3.times {
      def file = Files.createTempFile(dir, 'pactfile', '.json')
      file.write(pact)
    }
    mojo.pactDirectory = dir.toString()

    when:
    mojo.execute()

    then:
    3 * brokerClient.uploadPactFile(_, _, []) >> 'OK' >> 'FAILED! Bang' >> 'OK'
    thrown(MojoExecutionException)

    cleanup:
    dir.deleteDir()
  }

  def 'if the broker username is set, passes in the creds to the broker client'() {
    given:
    mojo.pactBrokerUsername = 'username'
    mojo.pactBrokerPassword = 'password'
    mojo.brokerClient = null
    mojo.pactBrokerUrl = '/broker'

    when:
    mojo.execute()

    then:
    new PactBrokerClient('/broker', _) >> { args ->
      assert args[1] == [authentication: ['basic', 'username', 'password']]
      brokerClient
    }
  }

    def 'trimSnapshot=true removes the "-SNAPSHOT"'() {
        given:
        mojo.projectVersion = '1.0.0-SNAPSHOT'
        mojo.trimSnapshot = true

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == '1.0.0'
    }

    def 'trimSnapshot=false leaves version unchanged'() {
        given:
        mojo.projectVersion = '1.0.0-SNAPSHOT'
        mojo.trimSnapshot = false

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == '1.0.0-SNAPSHOT'
    }

    def 'trimSnapshot=true leaves non-snapshot versions unchanged'() {
        given:
        mojo.projectVersion = '1.0.0'
        mojo.trimSnapshot = true

        when:
        mojo.execute()

        then:
        assert mojo.projectVersion == '1.0.0'
    }

  def 'Published the pacts to the pact broker with tags if any tags are specified'() {
    given:
    def dir = Files.createTempDirectory('pacts')
    def pact = PactPublishMojoSpec.classLoader.getResourceAsStream('pacts/contract.json').text
    def file = Files.createTempFile(dir, 'pactfile', '.json')
    file.write(pact)
    mojo.pactDirectory = dir.toString()

    def tags = ['one', 'two', 'three']
    mojo.tags = tags

    when:
    mojo.execute()

    then:
    1 * brokerClient.uploadPactFile(_, _, tags) >> 'OK'

    cleanup:
    dir.deleteDir()
  }

}
