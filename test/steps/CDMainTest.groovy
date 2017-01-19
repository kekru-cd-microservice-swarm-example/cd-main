package steps

import spock.lang.Specification;

class CDMainTest extends Specification {
    final cdMain = new CDMain()

    def 'extractImageTag refers to default' () {
        expect:
        cdMain.extractImageTag('manager1:5000/hello/world') == 'latest'
        cdMain.extractImageTag('hello/world') == 'latest'
    }

    def 'extractImageTag extracts tag from short image name'() {
        expect:
        cdMain.extractImageTag('hello:world') == 'world'
    }

    def 'extractImageTag extracts tag from full image name' () {
        expect:
        cdMain.extractImageTag('manager1:5000/hallo/welt:abc') == 'abc'
        cdMain.extractImageTag('manager1/hallo/welt:abc') == 'abc'
    }
}
