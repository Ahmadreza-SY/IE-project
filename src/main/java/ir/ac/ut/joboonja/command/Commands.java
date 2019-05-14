package ir.ac.ut.joboonja.command;

import ir.ac.ut.joboonja.entities.*;
import ir.ac.ut.joboonja.exceptions.BadRequestException;
import ir.ac.ut.joboonja.exceptions.ForbiddenException;
import ir.ac.ut.joboonja.exceptions.NotFoundException;
import ir.ac.ut.joboonja.models.BidAmount;
import ir.ac.ut.joboonja.entities.Bid;
import ir.ac.ut.joboonja.models.EndorsableSkill;
import ir.ac.ut.joboonja.repositories.*;
import ir.ac.ut.joboonja.repositories.impl.*;
import org.apache.commons.codec.cli.Digest;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Commands {

    private static AuctionRepository auctionRepository = new AuctionRepositoryImpl();
    private static UserRepository userRepository = new UserRepositoryImpl();
    private static ProjectRepository projectRepository = new ProjectRepositoryImpl();
    private static SkillRepository skillRepository = new SkillRepositoryImpl();
    private static EndorseRepository endorseRepository = new EndorseRepositoryImpl();

    public static void insertSkill(Skill skill) {
        skillRepository.insertSkill(skill);
    }
    public static List<User> getAllUsers() {
        User user = Commands.getDefaultUser();
        List<User> users = userRepository.getAllUsers();
        List<User> newList = new ArrayList<>(users);
        for(User u:newList){
            if(u.getId().equals(user.getId())){
                newList.remove(u);
                break;
            }
        }
        return newList;
    }

    private static List<Project> filterValidProjects(List<Project> allProjects, User user) {
        LinkedList<Project> result = new LinkedList<>();
        for(Project project: allProjects){
            if(hasEnoughSkills(user , project)) {
                result.add(project);
            }
        }
        return result;
    }

    public static List<Project> getValidProjects(User user) {
        return projectRepository.getAllProjects(user);
    }

    public static User getDefaultUser(){
        return userRepository.getUser("ali");
    }

    public static User getUserById(Integer id){
        User user = userRepository.getUserById(id);
        if (user == null)
            throw new NotFoundException("User not found");
        return user;
    }

    public static Project getProjectById(String id) {
        User user = getDefaultUser();
        Project project = projectRepository.getProjectById(id);
        if (project == null)
            throw new NotFoundException("Project not found!");
        if (!hasEnoughSkills(user, project))
            throw new ForbiddenException("Access to project is forbidden!");
        return project;
    }

    public static List<Skill> getAllSkills() {
        return skillRepository.getAllSkills();
    }

    private static boolean hasEnoughSkills(User user, Project project) {

        if (user == null || project == null)
            return false;

        boolean meets = true;
        for (Skill skill: project.getSkills()) {
            int skillIndex = user.getSkills().indexOf(skill);
            if (skillIndex == -1)
                meets = false;
            else if (user.getSkills().get(skillIndex).getPoint() < skill.getPoint())
                meets = false;
        }

        return meets;
    }

//    static void register(String json) throws SerializeException {
//        User user = Deserializer.deserialize(json , User.class);
//        userRepository.insertUser(user);
//    }
//
//    static void addProject(String json) throws SerializeException {
//        Project project = Deserializer.deserialize(json , Project.class);
//        projectRepository.insertProject(project);
//    }

    public static Bid addBid(Project project, Integer bidAmount) {
        User user = Commands.getDefaultUser();
        if (Commands.userIsBidBefore(project, user))
            throw new BadRequestException("User has already bidded!");
        if (bidAmount > project.getBudget())
            throw new BadRequestException("Bid amount is higher than project budget!");
        if (bidAmount < 0)
            throw new BadRequestException("Bid amount should be positive!");
        if (project.getDeadline() < (new Date()).getTime())
            throw new BadRequestException("Project deadline has been passed!");

        Bid bid = new Bid(user.getId(), project.getId(), bidAmount);
        auctionRepository.insertBid(bid);
        return bid;
    }

    public static boolean userIsBidBefore(Project project, User user){
        Auction auction = auctionRepository.getAuction(project.getId());
        if(auction == null) {
            return false;
        }
        else {
            for(Bid bid:auction.getOffers()) {
                if(bid.getUserId().equals(user.getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static BidAmount hasUserBid(Project project, User user){
        Auction auction = auctionRepository.getAuction(project.getId());
        BidAmount bidAmount = new BidAmount();
        if(auction == null) {
            bidAmount.setBidAmount(-1);
            return bidAmount;
        }
        else {
            for(Bid bid:auction.getOffers()) {
                if(bid.getUserId().equals(user.getId())) {
                    bidAmount.setBidAmount(bid.getBidAmount());
                    return bidAmount;
                }
            }
        }
        bidAmount.setBidAmount(-1);
        return bidAmount;
    }

//    private static boolean meetsRequirements(BidInfo bidInfo) {
//        User user = userRepository.getUser(bidInfo.getUserId());
//        Project project = projectRepository.getProject(bidInfo.getProjectTitle());
//        if (user == null || project == null)
//            return false;
//
//        if(bidInfo.getBidAmount() > project.getBudget())
//            return false;
//
//        return hasEnoughSkills(user , project);
//    }


    public static User auction(Project project){
        Auction auction = auctionRepository.getAuction(project.getId());
        User winnerUser = null;
        if (auction == null) {
            return winnerUser;
        }
        double maxPoint = 0;
        for(Bid bidInfo: auction.getOffers()){
            User user = userRepository.getUserById(bidInfo.getUserId());
            double point =  calAuctionPoint(project , user);
            point += project.getBudget() - bidInfo.getBidAmount();
            if(maxPoint < point) {
                maxPoint = point;
                winnerUser = user;
            }
        }

        return winnerUser;
    }

    private static double calAuctionPoint(Project project , User user){
        double sum = 0;

        for (Skill skill: project.getSkills()) {
            int skillIndex = user.getSkills().indexOf(skill);
            int userPoint = user.getSkills().get(skillIndex).getPoint();
            sum = 10000 * Math.pow((double) (userPoint - skill.getPoint()) , 2);
        }

        return sum;
    }

    public static void addUserSkill(String skillName) {
        User user = Commands.getDefaultUser();
        Skill skill = new Skill(skillName, 0);
        if (!skillRepository.skillExists(skill))
            throw new BadRequestException("Skill " + skillName + " doesn't exist!");
        if (user.getSkills().indexOf(skill) != -1)
            throw new BadRequestException("User already has " + skillName + " skill!");
        userRepository.addUserSkill(user.getId(), skillName);
    }

    public static void deleteUserSkill(String skillName) {
        User user = Commands.getDefaultUser();
        Skill skill = new Skill(skillName, 0);
        if (!skillRepository.skillExists(skill))
            throw new BadRequestException("Skill " + skillName + " doesn't exist!");
        if (user.getSkills().indexOf(skill) == -1)
            throw new BadRequestException("User doesn't have " + skillName + " skill!");
        userRepository.deleteUserSkill(user.getId(), skillName);
    }

    public static Endorse endorseSkill(Integer endorsedId, String skillName) {
        Integer endorserId = Commands.getDefaultUser().getId();
        Endorse endorse = new Endorse(endorserId, endorsedId, skillName);
        User user = getUserById(endorsedId);
        if (endorserId.equals(endorsedId))
            throw new ForbiddenException("You can't endorse yourself!");
        if (endorseRepository.endorseExists(endorse))
            throw new BadRequestException("Already Endorsed!");
        if (user.getSkills().indexOf(new Skill(skillName, 0)) == -1)
            throw new BadRequestException("User doesn't have endorsed skill!");

        endorseRepository.insertEndorse(endorse);
        userRepository.updateUserSkillPoint(endorsedId, skillName, 1);
        return endorse;
    }

    public static List<EndorsableSkill> getUserEndorsableSkills(Integer endorserId, Integer endorsedId) {
        User endorsed = userRepository.getUserById(endorsedId);

        if (endorsedId.equals(endorserId))
            return endorsed.getSkills().stream()
                .map(skill -> new EndorsableSkill(skill, false))
                .collect(Collectors.toList());

        List<Endorse> endorses = endorseRepository.getEndorses(endorserId);
        List<EndorsableSkill> result = new LinkedList<>();
        for (Skill skill: endorsed.getSkills()) {
            boolean endorsable = true;
            for (Endorse endorse : endorses)
                if (endorse.getEndorsedId().equals(endorsedId) && skill.getName().equals(endorse.getSkillName()))
                    endorsable = false;
            result.add(new EndorsableSkill(skill, endorsable));
        }
        return result;
    }

    public static Integer getUserBidAmount(Project project, User user) {
        Auction auction = auctionRepository.getAuction(project.getId());
        for(Bid bid:auction.getOffers()) {
            if(bid.getUserId().equals(user.getId())) {
                return bid.getBidAmount();
            }
        }
        return 0;
    }

    public static void insertProject(Project project) {
        projectRepository.insertProject(project);
    }

    public static List<User> searchUsers(String filter) {
        return userRepository.searchUsers(filter);
    }

    public static List<Project> searchValidProjects(String filter) {
        return projectRepository.searchProjects(filter);
    }

    public static List<Project> getValidProjects(User user, Integer pageNumber, Integer pageSize) {
        return projectRepository.getProjectsPaginated(user, pageNumber, pageSize);
    }

    public static List<Project> searchProjectsPaginated(String filter, Integer pageNumber, Integer pageSize) {
        return projectRepository.searchProjectsPaginated(filter, pageNumber, pageSize);
    }

    public static User insertUser(User newUser) {
        newUser.setPassword(DigestUtils.sha256Hex(newUser.getPassword().getBytes()));
        userRepository.insertUser(newUser);
        return newUser;
    }
}
