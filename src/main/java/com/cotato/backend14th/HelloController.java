@RestController
public class HelloController {

    @GetMapping("/hello")
    public String hello() {
        return "안녕하세요, 윤민호입니다!";
    }
}