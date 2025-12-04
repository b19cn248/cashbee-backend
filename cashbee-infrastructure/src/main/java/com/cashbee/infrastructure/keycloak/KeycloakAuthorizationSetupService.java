package com.cashbee.infrastructure.keycloak;

import com.cashbee.infrastructure.config.KeycloakProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.*;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Keycloak Authorization Setup Service.
 * <p>
 * This service programmatically sets up Keycloak Authorization Services including:
 * - Client Roles (USER, ADMIN)
 * - Authorization Scopes (view, create, update, delete, admin, etc.)
 * - Resources (API endpoints grouped by functionality)
 * - Policies (Role-based policies for USER and ADMIN)
 * - Permissions (Linking resources, scopes, and policies)
 * <p>
 * Uses Keycloak Admin Client to interact with Keycloak Authorization Services API.
 *
 * @author CashBee Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthorizationSetupService {

    private final KeycloakProperties keycloakProperties;

    private Keycloak keycloak;

    // ============================================================
    // CONSTANTS - Role Names
    // ============================================================
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    // ============================================================
    // CONSTANTS - Scope Names
    // ============================================================
    // Common scopes
    public static final String SCOPE_VIEW = "view";
    public static final String SCOPE_CREATE = "create";
    public static final String SCOPE_UPDATE = "update";
    public static final String SCOPE_DELETE = "delete";

    // User scopes
    public static final String SCOPE_USER_SYNC = "user:sync";
    public static final String SCOPE_USER_VIEW = "user:view";

    // Wallet scopes
    public static final String SCOPE_WALLET_VIEW = "wallet:view";
    public static final String SCOPE_WALLET_ADMIN = "wallet:admin";
    public static final String SCOPE_WALLET_LOCK = "wallet:lock";

    // Transaction scopes
    public static final String SCOPE_TRANSACTION_VIEW = "transaction:view";

    // Payout scopes (User)
    public static final String SCOPE_PAYOUT_CREATE = "payout:create";
    public static final String SCOPE_PAYOUT_VIEW = "payout:view";
    public static final String SCOPE_PAYOUT_CANCEL = "payout:cancel";

    // Payout scopes (Admin)
    public static final String SCOPE_PAYOUT_ADMIN_VIEW = "payout:admin:view";
    public static final String SCOPE_PAYOUT_ADMIN_APPROVE = "payout:admin:approve";
    public static final String SCOPE_PAYOUT_ADMIN_REJECT = "payout:admin:reject";
    public static final String SCOPE_PAYOUT_ADMIN_COMPLETE = "payout:admin:complete";

    // Admin scopes
    public static final String SCOPE_ADMIN_STATISTICS = "admin:statistics";
    public static final String SCOPE_ADMIN_USERS = "admin:users";
    public static final String SCOPE_ADMIN_IMPORT = "admin:import";
    public static final String SCOPE_ADMIN_PLATFORMS = "admin:platforms";
    public static final String SCOPE_ADMIN_BATCH_TRANSFER = "admin:batch-transfer";
    public static final String SCOPE_ADMIN_CASHBACK_POLICIES = "admin:cashback-policies";
    public static final String SCOPE_ADMIN_ORDERS = "admin:orders";

    // ============================================================
    // INITIALIZATION
    // ============================================================

    @PostConstruct
    public void init() {
        log.info("Initializing Keycloak Authorization Setup Service...");
        initKeycloak();
    }

    @PreDestroy
    public void destroy() {
        if (keycloak != null) {
            keycloak.close();
        }
    }

    private void initKeycloak() {
        try {
            KeycloakBuilder builder = KeycloakBuilder.builder()
                    .serverUrl(keycloakProperties.getServerUrl())
                    .realm(keycloakProperties.getRealm())
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS);

            if (keycloakProperties.getClientSecret() != null &&
                    !keycloakProperties.getClientSecret().isBlank()) {
                builder.clientId(keycloakProperties.getClientId())
                        .clientSecret(keycloakProperties.getClientSecret());
            } else {
                builder.clientId("admin-cli")
                        .username(keycloakProperties.getUsername())
                        .password(keycloakProperties.getPassword());
            }

            this.keycloak = builder.build();
            log.info("Keycloak Admin Client initialized for Authorization Setup");
        } catch (Exception e) {
            log.error("Failed to initialize Keycloak Admin Client", e);
            throw new RuntimeException("Keycloak initialization failed", e);
        }
    }

    // ============================================================
    // MAIN SETUP METHOD
    // ============================================================

    /**
     * Run the complete authorization setup.
     * This method creates all roles, scopes, resources, policies, and permissions
     * in Keycloak for the cashbee-backend client.
     *
     * @return SetupResult with details of what was created
     */
    public SetupResult runSetup() {
        log.info("========================================");
        log.info("Starting Keycloak Authorization Setup...");
        log.info("========================================");

        SetupResult result = new SetupResult();

        try {
            // Get client
            ClientRepresentation client = getClient();
            if (client == null) {
                throw new RuntimeException("Client '" + keycloakProperties.getClientId() + "' not found");
            }

            String clientId = client.getId();
            log.info("Found client: {} (ID: {})", client.getClientId(), clientId);

            // Enable Authorization Services on client if not already enabled
            enableAuthorizationServices(client);

            // Step 1: Create Client Roles
            log.info("--- Step 1: Creating Client Roles ---");
            createClientRoles(clientId, result);

            // Step 2: Create Authorization Scopes
            log.info("--- Step 2: Creating Authorization Scopes ---");
            createAuthorizationScopes(clientId, result);

            // Step 3: Create Resources
            log.info("--- Step 3: Creating Resources ---");
            createResources(clientId, result);

            // Step 4: Create Policies
            log.info("--- Step 4: Creating Policies ---");
            createPolicies(clientId, result);

            // Step 5: Create Permissions
            log.info("--- Step 5: Creating Permissions ---");
            createPermissions(clientId, result);

            result.setSuccess(true);
            log.info("========================================");
            log.info("Keycloak Authorization Setup Completed!");
            log.info("========================================");

        } catch (Exception e) {
            log.error("Authorization setup failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private RealmResource getRealmResource() {
        return keycloak.realm(keycloakProperties.getRealm());
    }

    private ClientRepresentation getClient() {
        List<ClientRepresentation> clients = getRealmResource()
                .clients()
                .findByClientId(keycloakProperties.getClientId());
        return clients.isEmpty() ? null : clients.get(0);
    }

    private void enableAuthorizationServices(ClientRepresentation client) {
        if (!Boolean.TRUE.equals(client.getAuthorizationServicesEnabled())) {
            log.info("Enabling Authorization Services on client...");
            client.setAuthorizationServicesEnabled(true);
            client.setServiceAccountsEnabled(true);
            getRealmResource().clients().get(client.getId()).update(client);
            log.info("Authorization Services enabled");
        } else {
            log.info("Authorization Services already enabled");
        }
    }

    private AuthorizationResource getAuthorizationResource(String clientId) {
        return getRealmResource().clients().get(clientId).authorization();
    }

    // ============================================================
    // STEP 1: CREATE CLIENT ROLES
    // ============================================================

    private void createClientRoles(String clientId, SetupResult result) {
        RolesResource rolesResource = getRealmResource().clients().get(clientId).roles();

        createClientRoleIfNotExists(rolesResource, ROLE_USER,
                "Regular user role with basic access to own data", result);
        createClientRoleIfNotExists(rolesResource, ROLE_ADMIN,
                "Administrator role with full access to manage users, orders, payouts", result);
    }

    private void createClientRoleIfNotExists(RolesResource rolesResource, String roleName,
                                             String description, SetupResult result) {
        try {
            rolesResource.get(roleName).toRepresentation();
            log.info("Role '{}' already exists", roleName);
            result.addExistingRole(roleName);
        } catch (Exception e) {
            // Role doesn't exist, create it
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleName);
            role.setDescription(description);
            role.setClientRole(true);
            rolesResource.create(role);
            log.info("Created role: {}", roleName);
            result.addCreatedRole(roleName);
        }
    }

    // ============================================================
    // STEP 2: CREATE AUTHORIZATION SCOPES
    // ============================================================

    private void createAuthorizationScopes(String clientId, SetupResult result) {
        AuthorizationResource authzResource = getAuthorizationResource(clientId);

        // Load existing scopes once (cache for efficiency)
        Set<String> existingScopeNames = new HashSet<>();
        try {
            List<ScopeRepresentation> existingScopes = authzResource.scopes().scopes();
            existingScopes.forEach(s -> existingScopeNames.add(s.getName()));
            log.info("Found {} existing scopes in Keycloak", existingScopeNames.size());
        } catch (Exception e) {
            log.warn("Could not load existing scopes: {}", e.getMessage());
        }

        // All scopes to create
        List<String> allScopes = Arrays.asList(
                // Common scopes
                SCOPE_VIEW, SCOPE_CREATE, SCOPE_UPDATE, SCOPE_DELETE,
                // User scopes
                SCOPE_USER_SYNC, SCOPE_USER_VIEW,
                // Wallet scopes
                SCOPE_WALLET_VIEW, SCOPE_WALLET_ADMIN, SCOPE_WALLET_LOCK,
                // Transaction scopes
                SCOPE_TRANSACTION_VIEW,
                // Payout scopes (User)
                SCOPE_PAYOUT_CREATE, SCOPE_PAYOUT_VIEW, SCOPE_PAYOUT_CANCEL,
                // Payout scopes (Admin)
                SCOPE_PAYOUT_ADMIN_VIEW, SCOPE_PAYOUT_ADMIN_APPROVE,
                SCOPE_PAYOUT_ADMIN_REJECT, SCOPE_PAYOUT_ADMIN_COMPLETE,
                // Admin scopes
                SCOPE_ADMIN_STATISTICS, SCOPE_ADMIN_USERS, SCOPE_ADMIN_IMPORT,
                SCOPE_ADMIN_PLATFORMS, SCOPE_ADMIN_BATCH_TRANSFER,
                SCOPE_ADMIN_CASHBACK_POLICIES, SCOPE_ADMIN_ORDERS
        );

        for (String scopeName : allScopes) {
            if (existingScopeNames.contains(scopeName)) {
                log.info("Scope '{}' already exists, skipping", scopeName);
                result.addExistingScope(scopeName);
            } else {
                createScope(authzResource, scopeName, result);
            }
        }
    }

    private void createScope(AuthorizationResource authzResource, String scopeName, SetupResult result) {
        try {
            ScopeRepresentation scope = new ScopeRepresentation();
            scope.setName(scopeName);
            scope.setDisplayName(formatDisplayName(scopeName));
            authzResource.scopes().create(scope);
            log.info("Created scope: {}", scopeName);
            result.addCreatedScope(scopeName);
        } catch (Exception e) {
            log.warn("Failed to create scope '{}': {}", scopeName, e.getMessage());
            result.addExistingScope(scopeName); // Likely already exists
        }
    }

    private String formatDisplayName(String scopeName) {
        // Convert "user:view" to "User View"
        return Arrays.stream(scopeName.replace(":", " ").replace("-", " ").split(" "))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                .reduce((a, b) -> a + " " + b)
                .orElse(scopeName);
    }

    // ============================================================
    // STEP 3: CREATE RESOURCES
    // ============================================================

    private void createResources(String clientId, SetupResult result) {
        ResourcesResource resourcesResource = getAuthorizationResource(clientId).resources();

        // User Resources
        createResourceIfNotExists(resourcesResource, "User Management",
                "urn:cashbee:resources:users", "/api/users/*",
                Arrays.asList(SCOPE_USER_SYNC, SCOPE_USER_VIEW, SCOPE_VIEW, SCOPE_UPDATE), result);

        // Wallet Resources
        createResourceIfNotExists(resourcesResource, "Wallet Management",
                "urn:cashbee:resources:wallets", "/api/wallets/*",
                Arrays.asList(SCOPE_WALLET_VIEW, SCOPE_WALLET_ADMIN, SCOPE_WALLET_LOCK), result);

        // Transaction Resources
        createResourceIfNotExists(resourcesResource, "Transaction History",
                "urn:cashbee:resources:transactions", "/api/transactions/*",
                Arrays.asList(SCOPE_TRANSACTION_VIEW), result);

        // Payout Resources (User)
        createResourceIfNotExists(resourcesResource, "User Payout Management",
                "urn:cashbee:resources:payouts:user", "/api/payouts/*",
                Arrays.asList(SCOPE_PAYOUT_CREATE, SCOPE_PAYOUT_VIEW, SCOPE_PAYOUT_CANCEL), result);

        // Payout Resources (Admin)
        createResourceIfNotExists(resourcesResource, "Admin Payout Management",
                "urn:cashbee:resources:payouts:admin", "/api/payouts/admin/*",
                Arrays.asList(SCOPE_PAYOUT_ADMIN_VIEW, SCOPE_PAYOUT_ADMIN_APPROVE,
                        SCOPE_PAYOUT_ADMIN_REJECT, SCOPE_PAYOUT_ADMIN_COMPLETE), result);

        // Order Resources (User)
        createResourceIfNotExists(resourcesResource, "User Orders",
                "urn:cashbee:resources:orders:user", "/api/orders/me",
                Arrays.asList(SCOPE_VIEW), result);

        // Order Resources (Admin)
        createResourceIfNotExists(resourcesResource, "Admin Order Management",
                "urn:cashbee:resources:orders:admin", "/api/admin/orders/*",
                Arrays.asList(SCOPE_ADMIN_ORDERS, SCOPE_VIEW, SCOPE_CREATE, SCOPE_UPDATE), result);

        // Admin Dashboard
        createResourceIfNotExists(resourcesResource, "Admin Dashboard",
                "urn:cashbee:resources:admin:dashboard", "/api/admin/dashboard/*",
                Arrays.asList(SCOPE_ADMIN_STATISTICS), result);

        // Admin Users
        createResourceIfNotExists(resourcesResource, "Admin Users",
                "urn:cashbee:resources:admin:users", "/api/admin/users/*",
                Arrays.asList(SCOPE_ADMIN_USERS, SCOPE_VIEW), result);

        // Admin Import
        createResourceIfNotExists(resourcesResource, "Admin Import",
                "urn:cashbee:resources:admin:import", "/api/admin/import/*",
                Arrays.asList(SCOPE_ADMIN_IMPORT), result);

        // Admin Platforms
        createResourceIfNotExists(resourcesResource, "Admin Platforms",
                "urn:cashbee:resources:admin:platforms", "/api/admin/platforms/*",
                Arrays.asList(SCOPE_ADMIN_PLATFORMS, SCOPE_VIEW, SCOPE_UPDATE), result);

        // Admin Batch Transfer
        createResourceIfNotExists(resourcesResource, "Admin Batch Transfer",
                "urn:cashbee:resources:admin:batch-transfer", "/api/admin/batch-transfer/*",
                Arrays.asList(SCOPE_ADMIN_BATCH_TRANSFER, SCOPE_VIEW, SCOPE_CREATE), result);

        // Admin Cashback Policies
        createResourceIfNotExists(resourcesResource, "Admin Cashback Policies",
                "urn:cashbee:resources:admin:cashback-policies", "/api/admin/cashback-policies/*",
                Arrays.asList(SCOPE_ADMIN_CASHBACK_POLICIES, SCOPE_VIEW, SCOPE_UPDATE), result);

        // Banks (Public read for authenticated users)
        createResourceIfNotExists(resourcesResource, "Banks",
                "urn:cashbee:resources:banks", "/api/banks/*",
                Arrays.asList(SCOPE_VIEW), result);

        // Affiliate Tracking (Create link for users)
        createResourceIfNotExists(resourcesResource, "Affiliate Tracking",
                "urn:cashbee:resources:affiliate:tracking", "/api/affiliate/tracking/*",
                Arrays.asList(SCOPE_VIEW, SCOPE_CREATE), result);
    }

    private void createResourceIfNotExists(ResourcesResource resourcesResource, String name,
                                           String type, String uri, List<String> scopeNames,
                                           SetupResult result) {
        // Check if resource exists
        try {
            List<ResourceRepresentation> existingResources = resourcesResource.findByName(name);
            if (!existingResources.isEmpty()) {
                log.info("Resource '{}' already exists, skipping", name);
                result.addExistingResource(name);
                return;
            }
        } catch (Exception e) {
            log.debug("Error checking resource existence: {}", e.getMessage());
        }

        // Create resource
        try {
            ResourceRepresentation resource = new ResourceRepresentation();
            resource.setName(name);
            resource.setDisplayName(name);
            resource.setType(type);
            resource.setUris(Set.of(uri));

            // Add scopes
            Set<ScopeRepresentation> scopes = new HashSet<>();
            for (String scopeName : scopeNames) {
                ScopeRepresentation scope = new ScopeRepresentation();
                scope.setName(scopeName);
                scopes.add(scope);
            }
            resource.setScopes(scopes);

            resourcesResource.create(resource);
            log.info("Created resource: {} with scopes: {}", name, scopeNames);
            result.addCreatedResource(name);
        } catch (Exception e) {
            log.warn("Failed to create resource '{}': {}", name, e.getMessage());
            result.addExistingResource(name); // Likely already exists
        }
    }

    // ============================================================
    // STEP 4: CREATE POLICIES
    // ============================================================

    private void createPolicies(String clientId, SetupResult result) {
        PoliciesResource policiesResource = getAuthorizationResource(clientId).policies();

        // Get client role IDs
        RolesResource rolesResource = getRealmResource().clients().get(clientId).roles();

        String userRoleId = getRoleId(rolesResource, ROLE_USER);
        String adminRoleId = getRoleId(rolesResource, ROLE_ADMIN);

        // Create User Policy (for regular users)
        createRolePolicyIfNotExists(policiesResource, "User Policy",
                "Policy for regular users with USER role",
                userRoleId, true, result);

        // Create Admin Policy (for administrators)
        createRolePolicyIfNotExists(policiesResource, "Admin Policy",
                "Policy for administrators with ADMIN role",
                adminRoleId, true, result);

        // Create User or Admin Policy (either role is sufficient)
        createAggregatedPolicyIfNotExists(policiesResource, "User or Admin Policy",
                "Policy that allows either USER or ADMIN role",
                Arrays.asList("User Policy", "Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);
    }

    private String getRoleId(RolesResource rolesResource, String roleName) {
        try {
            return rolesResource.get(roleName).toRepresentation().getId();
        } catch (Exception e) {
            log.error("Failed to get role ID for: {}", roleName);
            throw new RuntimeException("Role not found: " + roleName, e);
        }
    }

    private void createRolePolicyIfNotExists(PoliciesResource policiesResource, String policyName,
                                             String description, String roleId, boolean required,
                                             SetupResult result) {
        // Check if policy exists
        try {
            List<PolicyRepresentation> existingPolicies = policiesResource.policies()
                    .stream()
                    .filter(p -> p.getName().equals(policyName))
                    .toList();

            if (!existingPolicies.isEmpty()) {
                log.info("Policy '{}' already exists, skipping", policyName);
                result.addExistingPolicy(policyName);
                return;
            }
        } catch (Exception e) {
            log.debug("Error checking policy existence: {}", e.getMessage());
        }

        // Create role policy
        try {
            RolePolicyRepresentation policy = new RolePolicyRepresentation();
            policy.setName(policyName);
            policy.setDescription(description);
            policy.setLogic(Logic.POSITIVE);

            // Configure role
            RolePolicyRepresentation.RoleDefinition roleDefinition =
                    new RolePolicyRepresentation.RoleDefinition();
            roleDefinition.setId(roleId);
            roleDefinition.setRequired(required);
            policy.setRoles(Set.of(roleDefinition));

            policiesResource.role().create(policy);
            log.info("Created role policy: {}", policyName);
            result.addCreatedPolicy(policyName);
        } catch (Exception e) {
            log.warn("Failed to create role policy '{}': {}", policyName, e.getMessage());
            result.addExistingPolicy(policyName); // Likely already exists
        }
    }

    private void createAggregatedPolicyIfNotExists(PoliciesResource policiesResource, String policyName,
                                                   String description, List<String> policyNames,
                                                   DecisionStrategy decisionStrategy,
                                                   SetupResult result) {
        // Check if policy exists
        try {
            List<PolicyRepresentation> existingPolicies = policiesResource.policies()
                    .stream()
                    .filter(p -> p.getName().equals(policyName))
                    .toList();

            if (!existingPolicies.isEmpty()) {
                log.info("Aggregated policy '{}' already exists, skipping", policyName);
                result.addExistingPolicy(policyName);
                return;
            }
        } catch (Exception e) {
            log.debug("Error checking aggregated policy existence: {}", e.getMessage());
        }

        // Create aggregated policy
        try {
            AggregatePolicyRepresentation policy = new AggregatePolicyRepresentation();
            policy.setName(policyName);
            policy.setDescription(description);
            policy.setLogic(Logic.POSITIVE);
            policy.setDecisionStrategy(decisionStrategy);
            policy.setPolicies(new HashSet<>(policyNames));

            policiesResource.aggregate().create(policy);
            log.info("Created aggregated policy: {}", policyName);
            result.addCreatedPolicy(policyName);
        } catch (Exception e) {
            log.warn("Failed to create aggregated policy '{}': {}", policyName, e.getMessage());
            result.addExistingPolicy(policyName); // Likely already exists
        }
    }

    // ============================================================
    // STEP 5: CREATE PERMISSIONS
    // ============================================================

    private void createPermissions(String clientId, SetupResult result) {
        PermissionsResource permissionsResource = getAuthorizationResource(clientId).permissions();

        // ========== USER PERMISSIONS ==========

        // Users can view their own data
        createScopePermissionIfNotExists(permissionsResource,
                "User View Own Data Permission",
                "Allows users to view their own user data",
                Set.of("User Management"),
                Set.of(SCOPE_USER_VIEW, SCOPE_VIEW),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // Users can view their wallet
        createScopePermissionIfNotExists(permissionsResource,
                "User Wallet View Permission",
                "Allows users to view their wallet",
                Set.of("Wallet Management"),
                Set.of(SCOPE_WALLET_VIEW),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // Users can view transactions
        createScopePermissionIfNotExists(permissionsResource,
                "User Transaction View Permission",
                "Allows users to view their transactions",
                Set.of("Transaction History"),
                Set.of(SCOPE_TRANSACTION_VIEW),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // Users can manage their payouts
        createScopePermissionIfNotExists(permissionsResource,
                "User Payout Permission",
                "Allows users to create, view and cancel their payouts",
                Set.of("User Payout Management"),
                Set.of(SCOPE_PAYOUT_CREATE, SCOPE_PAYOUT_VIEW, SCOPE_PAYOUT_CANCEL),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // Users can view their orders
        createScopePermissionIfNotExists(permissionsResource,
                "User Orders View Permission",
                "Allows users to view their orders",
                Set.of("User Orders"),
                Set.of(SCOPE_VIEW),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // Users can view banks
        createScopePermissionIfNotExists(permissionsResource,
                "Banks View Permission",
                "Allows authenticated users to view banks",
                Set.of("Banks"),
                Set.of(SCOPE_VIEW),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // Users can create tracking links
        createScopePermissionIfNotExists(permissionsResource,
                "Affiliate Tracking Permission",
                "Allows users to create and view tracking links",
                Set.of("Affiliate Tracking"),
                Set.of(SCOPE_VIEW, SCOPE_CREATE),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);

        // ========== ADMIN-ONLY PERMISSIONS ==========

        // Admin wallet operations
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Wallet Operations Permission",
                "Allows admins to manage wallets (add, lock, unlock, deduct)",
                Set.of("Wallet Management"),
                Set.of(SCOPE_WALLET_ADMIN, SCOPE_WALLET_LOCK),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin payout operations
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Payout Operations Permission",
                "Allows admins to approve, reject, and complete payouts",
                Set.of("Admin Payout Management"),
                Set.of(SCOPE_PAYOUT_ADMIN_VIEW, SCOPE_PAYOUT_ADMIN_APPROVE,
                        SCOPE_PAYOUT_ADMIN_REJECT, SCOPE_PAYOUT_ADMIN_COMPLETE),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin dashboard
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Dashboard Permission",
                "Allows admins to view dashboard statistics",
                Set.of("Admin Dashboard"),
                Set.of(SCOPE_ADMIN_STATISTICS),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin users management
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Users Permission",
                "Allows admins to view and manage users",
                Set.of("Admin Users"),
                Set.of(SCOPE_ADMIN_USERS, SCOPE_VIEW),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin import
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Import Permission",
                "Allows admins to import orders",
                Set.of("Admin Import"),
                Set.of(SCOPE_ADMIN_IMPORT),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin platforms
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Platforms Permission",
                "Allows admins to manage affiliate platforms",
                Set.of("Admin Platforms"),
                Set.of(SCOPE_ADMIN_PLATFORMS, SCOPE_VIEW, SCOPE_UPDATE),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin batch transfer
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Batch Transfer Permission",
                "Allows admins to manage batch transfers",
                Set.of("Admin Batch Transfer"),
                Set.of(SCOPE_ADMIN_BATCH_TRANSFER, SCOPE_VIEW, SCOPE_CREATE),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin cashback policies
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Cashback Policies Permission",
                "Allows admins to manage cashback policies",
                Set.of("Admin Cashback Policies"),
                Set.of(SCOPE_ADMIN_CASHBACK_POLICIES, SCOPE_VIEW, SCOPE_UPDATE),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // Admin orders
        createScopePermissionIfNotExists(permissionsResource,
                "Admin Orders Permission",
                "Allows admins to manage orders",
                Set.of("Admin Order Management"),
                Set.of(SCOPE_ADMIN_ORDERS, SCOPE_VIEW, SCOPE_CREATE, SCOPE_UPDATE),
                Set.of("Admin Policy"),
                DecisionStrategy.UNANIMOUS, result);

        // User sync (admin operation)
        createScopePermissionIfNotExists(permissionsResource,
                "User Sync Permission",
                "Allows syncing users from Keycloak",
                Set.of("User Management"),
                Set.of(SCOPE_USER_SYNC),
                Set.of("User or Admin Policy"),
                DecisionStrategy.AFFIRMATIVE, result);
    }

    private void createScopePermissionIfNotExists(PermissionsResource permissionsResource,
                                                  String permissionName, String description,
                                                  Set<String> resourceNames, Set<String> scopeNames,
                                                  Set<String> policyNames,
                                                  DecisionStrategy decisionStrategy,
                                                  SetupResult result) {
        // Check if permission exists by trying to find scope permissions
        try {
            List<ScopePermissionRepresentation> existingPermissions = permissionsResource.scope().findAll(
                    permissionName, null, null, -1, -1);

            if (!existingPermissions.isEmpty()) {
                log.info("Permission '{}' already exists", permissionName);
                result.addExistingPermission(permissionName);
                return;
            }
        } catch (Exception e) {
            log.debug("Error checking permission existence: {}", e.getMessage());
        }

        // Create scope permission
        try {
            ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
            permission.setName(permissionName);
            permission.setDescription(description);
            permission.setLogic(Logic.POSITIVE);
            permission.setDecisionStrategy(decisionStrategy);
            permission.setResources(resourceNames);
            permission.setScopes(scopeNames);
            permission.setPolicies(policyNames);

            permissionsResource.scope().create(permission);
            log.info("Created permission: {}", permissionName);
            result.addCreatedPermission(permissionName);
        } catch (Exception e) {
            log.warn("Failed to create permission '{}': {}", permissionName, e.getMessage());
            result.addExistingPermission(permissionName); // Likely already exists
        }
    }

    // ============================================================
    // RESULT CLASS
    // ============================================================

    /**
     * Result of the authorization setup process.
     */
    public static class SetupResult {
        private boolean success;
        private String errorMessage;

        private final List<String> createdRoles = new ArrayList<>();
        private final List<String> existingRoles = new ArrayList<>();
        private final List<String> createdScopes = new ArrayList<>();
        private final List<String> existingScopes = new ArrayList<>();
        private final List<String> createdResources = new ArrayList<>();
        private final List<String> existingResources = new ArrayList<>();
        private final List<String> createdPolicies = new ArrayList<>();
        private final List<String> existingPolicies = new ArrayList<>();
        private final List<String> createdPermissions = new ArrayList<>();
        private final List<String> existingPermissions = new ArrayList<>();

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public List<String> getCreatedRoles() { return createdRoles; }
        public List<String> getExistingRoles() { return existingRoles; }
        public List<String> getCreatedScopes() { return createdScopes; }
        public List<String> getExistingScopes() { return existingScopes; }
        public List<String> getCreatedResources() { return createdResources; }
        public List<String> getExistingResources() { return existingResources; }
        public List<String> getCreatedPolicies() { return createdPolicies; }
        public List<String> getExistingPolicies() { return existingPolicies; }
        public List<String> getCreatedPermissions() { return createdPermissions; }
        public List<String> getExistingPermissions() { return existingPermissions; }

        public void addCreatedRole(String role) { createdRoles.add(role); }
        public void addExistingRole(String role) { existingRoles.add(role); }
        public void addCreatedScope(String scope) { createdScopes.add(scope); }
        public void addExistingScope(String scope) { existingScopes.add(scope); }
        public void addCreatedResource(String resource) { createdResources.add(resource); }
        public void addExistingResource(String resource) { existingResources.add(resource); }
        public void addCreatedPolicy(String policy) { createdPolicies.add(policy); }
        public void addExistingPolicy(String policy) { existingPolicies.add(policy); }
        public void addCreatedPermission(String permission) { createdPermissions.add(permission); }
        public void addExistingPermission(String permission) { existingPermissions.add(permission); }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("success", success);
            if (errorMessage != null) {
                map.put("errorMessage", errorMessage);
            }
            map.put("summary", Map.of(
                    "rolesCreated", createdRoles.size(),
                    "rolesExisting", existingRoles.size(),
                    "scopesCreated", createdScopes.size(),
                    "scopesExisting", existingScopes.size(),
                    "resourcesCreated", createdResources.size(),
                    "resourcesExisting", existingResources.size(),
                    "policiesCreated", createdPolicies.size(),
                    "policiesExisting", existingPolicies.size(),
                    "permissionsCreated", createdPermissions.size(),
                    "permissionsExisting", existingPermissions.size()
            ));
            map.put("details", Map.of(
                    "createdRoles", createdRoles,
                    "existingRoles", existingRoles,
                    "createdScopes", createdScopes,
                    "existingScopes", existingScopes,
                    "createdResources", createdResources,
                    "existingResources", existingResources,
                    "createdPolicies", createdPolicies,
                    "existingPolicies", existingPolicies,
                    "createdPermissions", createdPermissions,
                    "existingPermissions", existingPermissions
            ));
            return map;
        }
    }
}
