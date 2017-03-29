/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat
 *
 *******************************************************************************/
package org.eclipse.kapua.service.device.registry.common;

import org.eclipse.kapua.KapuaException;
import org.eclipse.kapua.commons.model.query.predicate.AttributePredicate;
import org.eclipse.kapua.commons.security.KapuaSecurityUtils;
import org.eclipse.kapua.commons.util.ArgumentValidator;
import org.eclipse.kapua.locator.KapuaLocator;
import org.eclipse.kapua.model.KapuaEntity;
import org.eclipse.kapua.model.id.KapuaId;
import org.eclipse.kapua.model.query.KapuaQuery;
import org.eclipse.kapua.service.authorization.AuthorizationService;
import org.eclipse.kapua.service.authorization.domain.Domain;
import org.eclipse.kapua.service.authorization.group.Group;
import org.eclipse.kapua.service.authorization.group.GroupService;
import org.eclipse.kapua.service.authorization.permission.Actions;
import org.eclipse.kapua.service.authorization.permission.PermissionFactory;
import org.eclipse.kapua.service.device.registry.Device;
import org.eclipse.kapua.service.device.registry.DeviceCreator;
import org.eclipse.kapua.service.device.registry.DeviceFactory;
import org.eclipse.kapua.service.device.registry.DeviceListResult;
import org.eclipse.kapua.service.device.registry.DevicePredicates;
import org.eclipse.kapua.service.device.registry.DeviceQuery;
import org.eclipse.kapua.service.device.registry.DeviceRegistryService;
import org.eclipse.kapua.service.device.registry.internal.DeviceDomain;

/**
 * Provides logic used to validate preconditions required to execute the device service operation.
 * 
 * @since 1.0.0
 */
public final class DeviceValidation {

    private static final Domain deviceDomain = new DeviceDomain();

    private static final AuthorizationService authorizationService;
    private static final GroupService groupService;
    private static final PermissionFactory permissionFactory;

    private static final DeviceRegistryService deviceRegistryService;
    private static final DeviceFactory deviceFactory;

    static {
        authorizationService = KapuaLocator.getInstance().getService(AuthorizationService.class);
        groupService = KapuaLocator.getInstance().getService(GroupService.class);
        permissionFactory = KapuaLocator.getInstance().getFactory(PermissionFactory.class);

        deviceRegistryService = KapuaLocator.getInstance().getService(DeviceRegistryService.class);
        deviceFactory = KapuaLocator.getInstance().getFactory(DeviceFactory.class);
    }

    private DeviceValidation() {
    }

    /**
     * Validates the device creates precondition
     * 
     * @param deviceCreator
     * @return
     * @throws KapuaException
     */
    public static DeviceCreator validateCreatePreconditions(DeviceCreator deviceCreator) throws KapuaException {
        ArgumentValidator.notNull(deviceCreator, "deviceCreator");
        ArgumentValidator.notNull(deviceCreator.getScopeId(), "deviceCreator.scopeId");
        ArgumentValidator.notEmptyOrNull(deviceCreator.getClientId(), "deviceCreator.clientId");

        if (deviceCreator.getGroupId() != null) {
            ArgumentValidator.notNull(groupService.find(deviceCreator.getScopeId(), deviceCreator.getGroupId()), "deviceCreator.groupId");
        }

        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.write, deviceCreator.getScopeId(), deviceCreator.getGroupId()));

        return deviceCreator;
    }

    /**
     * Validates the device updates precondition
     * 
     * @param device
     * @return
     * @throws KapuaException
     */
    public static Device validateUpdatePreconditions(Device device) throws KapuaException {
        ArgumentValidator.notNull(device, "device");
        ArgumentValidator.notNull(device.getId(), "device.id");
        ArgumentValidator.notNull(device.getScopeId(), "device.scopeId");

        // Check that current user can manage the current group of the device
        KapuaId currentGroupId = findCurrentGroupId(device.getScopeId(), device.getId());
        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.write, device.getScopeId(), currentGroupId));

        // Check that current user can manage the target group of the device
        if (device.getGroupId() != null) {
            ArgumentValidator.notNull(groupService.find(device.getScopeId(), device.getGroupId()), "device.groupId");
        }
        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.write, device.getScopeId(), device.getGroupId()));

        return device;
    }

    /**
     * Validates the find device precondition
     * 
     * @param scopeId
     * @param entityId
     * @throws KapuaException
     */
    public static void validateFindPreconditions(KapuaId scopeId, KapuaId entityId) throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(entityId, "entityId");

        KapuaId groupId = findCurrentGroupId(scopeId, entityId);
        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.read, scopeId, groupId));
    }

    /**
     * Validates the device query precondition
     * 
     * @param query
     * @throws KapuaException
     */
    public static void validateQueryPreconditions(KapuaQuery<Device> query) throws KapuaException {
        ArgumentValidator.notNull(query, "query");
        ArgumentValidator.notNull(query.getScopeId(), "query.scopeId");

        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.read, query.getScopeId(), Group.ANY));
    }

    /**
     * Validates the device count precondition
     * 
     * @param query
     * @throws KapuaException
     */
    public static void validateCountPreconditions(KapuaQuery<Device> query) throws KapuaException {
        ArgumentValidator.notNull(query, "query");
        ArgumentValidator.notNull(query.getScopeId(), "query.scopeId");

        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.read, query.getScopeId(), Group.ANY));
    }

    /**
     * Validates the device delete precondition
     * 
     * @param scopeId
     * @param deviceId
     * @throws KapuaException
     */
    public static void validateDeletePreconditions(KapuaId scopeId, KapuaId deviceId) throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notNull(deviceId, "id");

        KapuaId groupId = findCurrentGroupId(scopeId, deviceId);
        authorizationService.checkPermission(permissionFactory.newPermission(deviceDomain, Actions.delete, scopeId, groupId));
    }

    /**
     * Validates the device find by identifier precondition
     * 
     * @param scopeId
     * @param clientId
     * @throws KapuaException
     * @since 1.0.0
     */
    public static void validateFindByClientIdPreconditions(KapuaId scopeId, String clientId) throws KapuaException {
        ArgumentValidator.notNull(scopeId, "scopeId");
        ArgumentValidator.notEmptyOrNull(clientId, "clientId");

        // Check access is performed by the query method.
    }

    /**
     * Finds the current {@link Group} id assigned to the given {@link Device} id.
     * 
     * @param scopeId
     *            The scope {@link KapuaId} of the {@link Device}
     * @param entityId
     *            The {@link KapuaEntity} {@link KapuaId} of the {@link Device}.
     * @return The {@link Group} id found.
     * @throws KapuaException
     * @since 1.0.0
     */
    private static KapuaId findCurrentGroupId(KapuaId scopeId, KapuaId entityId) throws KapuaException {
        //
        // FIXME introduce the capability in KapuaQuery to select single values instead of selecting the whole object.
        //

        DeviceQuery query = deviceFactory.newQuery(scopeId);
        query.setPredicate(new AttributePredicate<>(DevicePredicates.ENTITY_ID, entityId));

        DeviceListResult results = null;
        try {
            results = KapuaSecurityUtils.doPrivileged(() -> (DeviceListResult) deviceRegistryService.query(query));
        } catch (Exception e) {
            throw KapuaException.internalError(e, "Error while searching groupId");
        }

        KapuaId groupId = null;
        if (results != null && !results.isEmpty()) {
            groupId = results.getFirstItem().getGroupId();
        }

        return groupId;
    }
}
