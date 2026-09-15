@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "안녕하세요, [본인 이름]입니다!";
    }
}
