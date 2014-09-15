package com.taskadapter.redmineapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.taskadapter.redmineapi.bean.Group;
import com.taskadapter.redmineapi.bean.GroupFactory;
import com.taskadapter.redmineapi.bean.Membership;
import com.taskadapter.redmineapi.bean.MembershipFactory;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.ProjectFactory;
import com.taskadapter.redmineapi.bean.Role;
import com.taskadapter.redmineapi.bean.UserFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.taskadapter.redmineapi.bean.User;

public class UserIntegrationTest {
    private static final User OUR_USER = IntegrationTestHelper.getOurUser();

    private static RedmineManager mgr;
    private static UserManager userManager;

    private static String projectKey;
    private static Integer nonAdminUserId;
    private static String nonAdminUserLogin;
    private static String nonAdminPassword;

    @BeforeClass
    public static void oneTimeSetup() {
        mgr = IntegrationTestHelper.createRedmineManager();
        userManager = mgr.getUserManager();
        try {
            projectKey = IntegrationTestHelper.createProject(mgr);
            createNonAdminUser();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void oneTimeTearDown() {
        IntegrationTestHelper.deleteProject(mgr, projectKey);
    }

    private static void createNonAdminUser() throws RedmineException {
        User user = UserGenerator.generateRandomUser();
        User nonAdminUser = userManager.createUser(user);
        nonAdminUserId = nonAdminUser.getId();
        nonAdminUserLogin = user.getLogin();
        nonAdminPassword = user.getPassword();
    }

    @Test
    public void usersAreLoadedByAdmin() {
        try {
            List<User> users = userManager.getUsers();
            assertTrue(users.size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test(expected = NotAuthorizedException.class)
    public void usersCannotBeLoadedByNotAdmin() throws RedmineException {
        getNonAdminManager().getUserManager().getUsers();
        fail("Must have failed with NotAuthorizedException.");
    }

    @Test
    public void userCanBeLoadedByIdByNonAdmin() throws RedmineException {
        User userById = getNonAdminManager().getUserManager().getUserById(nonAdminUserId);
        assertEquals(nonAdminUserId, userById.getId());
    }

    @Test
    public void testGetCurrentUser() throws RedmineException {
        User currentUser = userManager.getCurrentUser();
        assertEquals(OUR_USER.getId(), currentUser.getId());
        assertEquals(OUR_USER.getLogin(), currentUser.getLogin());
    }

    @Test
    public void testGetUserById() throws RedmineException {
        User loadedUser = userManager.getUserById(OUR_USER.getId());
        assertEquals(OUR_USER.getId(), loadedUser.getId());
        assertEquals(OUR_USER.getLogin(), loadedUser.getLogin());
        assertEquals(OUR_USER.getApiKey(), loadedUser.getApiKey());
    }

    @Test(expected = NotFoundException.class)
    public void testGetUserNonExistingId() throws RedmineException {
        userManager.getUserById(999999);
    }

    @Test
    public void testCreateUser() throws RedmineException {
        User createdUser = null;
        try {
            User userToCreate = UserGenerator.generateRandomUser();
            createdUser = userManager.createUser(userToCreate);

            assertNotNull(
                    "checking that a non-null user is returned", createdUser);

            assertEquals(userToCreate.getLogin(), createdUser.getLogin());
            assertEquals(userToCreate.getFirstName(),
                    createdUser.getFirstName());
            assertEquals(userToCreate.getLastName(),
                    createdUser.getLastName());
            Integer id = createdUser.getId();
            assertNotNull(id);

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            if (createdUser != null) {
                userManager.deleteUser(createdUser.getId());
            }
        }
    }
    
    @Test
    public void testCreateUserWithAuthSource() throws RedmineException {
        User createdUser = null;
        try {
            User userToCreate = UserGenerator.generateRandomUser();
            userToCreate.setAuthSourceId(1);
            createdUser = userManager.createUser(userToCreate);

            assertNotNull("checking that a non-null user is returned", createdUser);
            
//            Redmine doesn't return it, so let's consider a non-exceptional return as success for now. 
//            assertNotNull("checking that a non-null auth_source_id is returned", createdUser.getAuthSourceId());
//            assertEquals(1, createdUser.getAuthSourceId().intValue());

        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            if (createdUser != null) {
                userManager.deleteUser(createdUser.getId());
            }
        }
    }

    @Test
    public void testUpdateUser() throws RedmineAuthenticationException,
            NotFoundException {
        User userToCreate = UserFactory.create();
        userToCreate.setFirstName("fname2");
        userToCreate.setLastName("lname2");
        long randomNumber = new Date().getTime();
        userToCreate.setLogin("login33" + randomNumber);
        userToCreate.setMail("email" + randomNumber + "@somedomain.com");
        userToCreate.setPassword("1234asdf");
        try {
            User createdUser = userManager.createUser(userToCreate);
            Integer userId = createdUser.getId();
            assertNotNull(
                    "checking that a non-null project is returned", createdUser);

            String newFirstName = "fnameNEW";
            String newLastName = "lnameNEW";
            String newMail = "newmail" + randomNumber + "@asd.com";
            createdUser.setFirstName(newFirstName);
            createdUser.setLastName(newLastName);
            createdUser.setMail(newMail);

            userManager.update(createdUser);

            User updatedUser = userManager.getUserById(userId);

            assertEquals(newFirstName, updatedUser.getFirstName());
            assertEquals(newLastName, updatedUser.getLastName());
            assertEquals(newMail, updatedUser.getMail());
            assertEquals(userId, updatedUser.getId());

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void userCanBeDeleted() throws RedmineException {
        User user = UserGenerator.generateRandomUser();
        User createdUser = userManager.createUser(user);
        Integer newUserId = createdUser.getId();

        try {
            userManager.deleteUser(newUserId);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        try {
            userManager.getUserById(newUserId);
            fail("Must have failed with NotFoundException because we tried to delete the user");
        } catch (NotFoundException e) {
            // ignore: the user should not be found
        }
    }

    @Test(expected = NotFoundException.class)
    public void deletingNonExistingUserThrowsNFE() throws RedmineException {
        userManager.deleteUser(999999);
    }

    /**
     * Requires Redmine 2.1
     */
    @Test
    public void testAddUserToGroup() throws RedmineException {
        final Group template = GroupFactory.create();
        template.setName("testAddUserToGroup " + System.currentTimeMillis());
        final Group group = userManager.createGroup(template);
        try {
            final User newUser = userManager.createUser(UserGenerator.generateRandomUser());
            try {
                userManager.addUserToGroup(newUser, group);
                final Collection<Group> userGroups = userManager.getUserById(newUser.getId()).getGroups();
                assertTrue(userGroups.size() == 1);
                assertTrue(group.getName().equals(userGroups.iterator().next().getName()));
            } finally {
                userManager.deleteUser(newUser.getId());
            }
        } finally {
            userManager.deleteGroup(group);
        }
    }

    /**
     * Requires Redmine 2.1
     */
    @Test
    public void addingUserToGroupTwiceDoesNotGiveErrors() throws RedmineException {
        final Group template = GroupFactory.create();
        template.setName("some test " + System.currentTimeMillis());
        final Group group = userManager.createGroup(template);
        try {
            final User newUser = userManager.createUser(UserGenerator.generateRandomUser());
            try {
                userManager.addUserToGroup(newUser, group);
                userManager.addUserToGroup(newUser, group);
                assertTrue(userManager.getUserById(newUser.getId()).getGroups().size() == 1);
            } finally {
                userManager.deleteUser(newUser.getId());
            }
        } finally {
            userManager.deleteGroup(group);
        }
    }

    @Test
    public void testGroupCRUD() throws RedmineException {
        final Group template = GroupFactory.create();
        template.setName("Template group " + System.currentTimeMillis());
        final Group created = userManager.createGroup(template);

        try {
            assertEquals(template.getName(), created.getName());
            final Group loaded = userManager.getGroupById(created.getId());
            assertEquals(template.getName(), loaded.getName());

            final Group update = GroupFactory.create(loaded.getId());
            update.setName("Group update " + System.currentTimeMillis());

            userManager.update(update);

            final Group loaded2 = userManager.getGroupById(created.getId());
            assertEquals(update.getName(), loaded2.getName());
        } finally {
            userManager.deleteGroup(created);
        }

        try {
            userManager.getGroupById(created.getId());
            fail("Group should be deleted but was found");
        } catch (NotFoundException e) {
            // OK!
        }
    }

    @Test
    public void testMemberships() throws RedmineException {
        final List<Role> roles = mgr.getUserManager().getRoles();

        final Membership newMembership = MembershipFactory.create();
        final Project project = ProjectFactory.create();
        project.setIdentifier(projectKey);
        newMembership.setProject(project);
        final User currentUser = mgr.getUserManager().getCurrentUser();
        newMembership.setUser(currentUser);
        newMembership.addRoles(roles);

        userManager.addMembership(newMembership);
        final List<Membership> memberships1 = userManager.getMemberships(project);
        assertEquals(1, memberships1.size());
        final Membership createdMembership = memberships1.get(0);
        assertEquals(currentUser.getId(), createdMembership.getUser()
                .getId());
        assertEquals(roles.size(), createdMembership.getRoles().size());

        final Membership membershipById = userManager.getMembership(createdMembership
                .getId());
        assertEquals(createdMembership, membershipById);

        final Membership emptyMembership = MembershipFactory.create(createdMembership.getId());
        emptyMembership.setProject(createdMembership.getProject());
        emptyMembership.setUser(createdMembership.getUser());
        emptyMembership.addRoles(Collections.singletonList(roles.get(0)));

        userManager.update(emptyMembership);
        final Membership updatedEmptyMembership = userManager.getMembership(createdMembership.getId());

        assertEquals(1, updatedEmptyMembership.getRoles().size());
        userManager.delete(updatedEmptyMembership);
    }

    @Test
    public void testUserMemberships() throws RedmineException {
        final List<Role> roles = userManager.getRoles();
        final Membership newMembership = MembershipFactory.create();

        final Project project = ProjectFactory.create();
        project.setIdentifier(projectKey);
        newMembership.setProject(project);
        final User currentUser = userManager.getCurrentUser();
        newMembership.setUser(currentUser);
        newMembership.addRoles(roles);

        userManager.addMembership(newMembership);

        final User userWithMembership = userManager.getUserById(currentUser.getId());
        assertTrue(userWithMembership.getMemberships().size() > 0);
    }

    @Test
    public void testGetRoleById() throws RedmineException {
        final Collection<Role> roles = userManager.getRoles();
        for (Role r : roles) {
            final Role loaded = userManager.getRoleById(r.getId());
            assertEquals(r.getName(), loaded.getName());
            assertEquals(r.getInherited(), loaded.getInherited());
        }
    }

    @Test
    public void testRolesHasPermissions() throws RedmineException {
        final Collection<Role> roles = userManager.getRoles();
        for (Role r : roles) {
            final Role loaded = userManager.getRoleById(r.getId());
            if (loaded.getPermissions() != null && !loaded.getPermissions().isEmpty())
                return;

        }
        fail("Failed to find a role with a permissions");
    }

    @Test
    public void testGetRoles() throws RedmineException {
        assertTrue(userManager.getRoles().size() > 0);
    }

    @Test
    public void testUserDefaults() throws RedmineException {
        final User template = UserFactory.create();
        template.setFirstName("first name");
        template.setLastName("last name");
        template.setMail("root@globalhost.ru");
        template.setPassword("aslkdj32jnrfds7asdfn23()[]:kajsdf");
        template.setLogin("asdNnadnNasd");
        final User result = userManager.createUser(template);
        try {
            Assert.assertNotNull(result.getId());
            Assert.assertEquals("asdNnadnNasd", result.getLogin());
            Assert.assertNull(result.getPassword());
            Assert.assertEquals("first name", result.getFirstName());
            Assert.assertEquals("last name", result.getLastName());
            Assert.assertEquals("root@globalhost.ru", result.getMail());
            Assert.assertNotNull(result.getCreatedOn());
            Assert.assertNull(result.getLastLoginOn());
            Assert.assertNotNull(result.getCustomFields());
        } finally {
            userManager.deleteUser(result.getId());
        }
    }

    private RedmineManager getNonAdminManager() {
        return RedmineManagerFactory.createWithUserAuth(IntegrationTestHelper.getTestConfig().getURI(),
                    nonAdminUserLogin, nonAdminPassword);
    }

}