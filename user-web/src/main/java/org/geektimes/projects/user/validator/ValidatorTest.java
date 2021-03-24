package org.geektimes.projects.user.validator;

import org.geektimes.projects.user.domain.User;
import org.geektimes.web.mvc.validator.ValidatorDelegate;

import java.util.Map;

/**
 * 2021/3/7
 * jizhi7
 **/
public class ValidatorTest {

    public static void main(String[] args) {

        User user = new User();

        ValidatorDelegate v = new ValidatorDelegate();
        Map<String, String> re = v.validate(user);
        System.out.println(re);

    }

}
