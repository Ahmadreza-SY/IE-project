package ir.ac.ut.joboonja.controllers;

import ir.ac.ut.joboonja.command.Commands;
import ir.ac.ut.joboonja.entities.Endorse;
import ir.ac.ut.joboonja.entities.Skill;
import ir.ac.ut.joboonja.entities.User;
import ir.ac.ut.joboonja.models.EndorsableSkill;
import ir.ac.ut.joboonja.models.EndorseRequest;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping
    public List<User> getUsers() {
        return Commands.getAllUsers();
    }

    @GetMapping("/{userId}")
    public User getUser(@PathVariable("userId") Integer userId) {
        return Commands.getUserById(userId);
    }

    @GetMapping("/{userId}/endorse")
    public List<EndorsableSkill> getEndorsableSkills(@PathVariable("userId") Integer userId) {
        return Commands.getUserEndorsableSkills(Commands.getDefaultUser().getId(), userId);
    }

    @PostMapping("/{userId}/endorse")
    public Endorse endorse(@PathVariable("userId") Integer userId, @RequestBody EndorseRequest endorseRequest) {
        return Commands.endorseSkill(userId, endorseRequest.getSkillName());
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        if (user.getSkills().size() != 0) {
            for (Skill skill: user.getSkills()) {
                Commands.addUserSkill(skill.getName());
            }
        }
        return Commands.getDefaultUser();
    }

    @DeleteMapping
    public User deleteUserSkill(@RequestBody User user) {
        if (user.getSkills().size() != 0) {
            for (Skill skill: user.getSkills()) {
                Commands.deleteUserSkill(skill.getName());
            }
        }
        return Commands.getDefaultUser();
    }
}
