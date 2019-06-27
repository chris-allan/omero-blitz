/*
 * Copyright (C) 2015-2019 University of Dundee & Open Microscopy Environment.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package ome.formats.importer.targets;

import static omero.rtypes.rstring;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportContainer;
import omero.api.IQueryPrx;
import omero.api.IUpdatePrx;
import omero.model.*;
import static omero.rtypes.rlong;
import omero.sys.ParametersI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 5.1.2
 */
public class ModelImportTarget implements ImportTarget {

    private final static Logger log = LoggerFactory.getLogger(ModelImportTarget.class);

    /**
     * Valid omero.model classes for model import target.
     */
    private static final List<String> VALID_TYPES = Arrays.asList(
            "omero.model.Project", "omero.model.Dataset", "omero.model.Screen");

    /**
     * omero.model class which can be used for instantiation.
     */
    private Class<? extends IObject> type;

    /**
     * Object of {@link #type} which is found or created.
     */
    private Long id;

    /**
     * Internal representation of a target deeper in the hierarchy if needed.
     * For example, if this instance represents a Project, then the subTarget
     * would be a Dataset.
     */
    private ModelImportTarget subTarget;

    /**
     * String used for querying the database; must be ome.model based.
     */
    private String typeName;

    private String simpleName;

    private String prefix;

    private String discriminator;

    private String name;

    @SuppressWarnings("unchecked")
    @Override
    public void init(String target) {
        // Builder is responsible for only passing valid files.
        String[] tokens = target.split(":",3);
        this.prefix = tokens[0];
        if (tokens.length == 2) {
            this.name = tokens[1];
            this.discriminator = "id";
        } else {
            this.name = tokens[2];
            this.discriminator = tokens[1];
        }
        type = tryClass(prefix);
        Class<?> k = omero.util.IceMap.OMEROtoOME.get(type);
        typeName = k.getName();
        simpleName = k.getSimpleName();
        // Reversing will take us from an abstract type to one constructible.
        type = omero.util.IceMap.OMEtoOMERO.get(k);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends IObject> tryClass(String prefix) {
        Class<? extends IObject> klass = null;
        try {
            klass = (Class<? extends IObject>) Class.forName(prefix);
        } catch (Exception e) {
            try {
                klass = (Class<? extends IObject>) Class.forName("omero.model."+prefix);
            } catch (Exception e1) {
                throw new RuntimeException("Unknown class:" + prefix);
            }
        }
        if (!VALID_TYPES.contains(klass.getName())) {
            throw new RuntimeException("Not a valid container class:" + klass.getName());
        }
        return klass;
    }

    public Class<? extends IObject> getObjectType() {
        if (subTarget != null) {
            return subTarget.getObjectType();
        }
        return type;
    }

    public Long getObjectId() {
        if (subTarget != null) {
            return subTarget.getObjectId();
        }
        return id;
    }

    @Override
    public IObject load(OMEROMetadataStoreClient client, ImportContainer ic) throws Exception {
        IQueryPrx query = client.getServiceFactory().getQueryService();
        IUpdatePrx update = client.getServiceFactory().getUpdateService();

        if ("Project".equals(simpleName)) {
            if (!name.contains("/")) {
                throw new RuntimeException("Project name is missing a '/': " + name);
            }
            String[] tokens = name.split("/", 2);
            name = tokens[0];
            log.info("Creating sub-target: {}", tokens[1]);
            subTarget = new ModelImportTarget();
            subTarget.init(tokens[1]);
        }

        if (discriminator.matches("^[-+%@]?name$")) {
            IObject obj;
            String order = "desc";
            if (discriminator.startsWith("-")) {
                order = "asc";
            }
            final List<IObject> objs;
            if (discriminator.startsWith("@")) {
                objs = Collections.emptyList();
            } else {
                objs = (List<IObject>) query.findAllByQuery(
                        "select o from " + simpleName + " as o where o.name = :name"
                                + " order by o.id " + order,
                                new ParametersI().add("name", rstring(name)));
                final Iterator<IObject> objIter = objs.iterator();
                while (objIter.hasNext()) {
                    if (!objIter.next().getDetails().getPermissions().canLink()) {
                        objIter.remove();
                    }
                }
            }
            if (objs.isEmpty()) {
                obj = type.newInstance();
                Method m = type.getMethod("setName", omero.RString.class);
                m.invoke(obj, rstring(name));
                obj = update.saveAndReturnObject(obj);
            } else {
                if (discriminator.startsWith("%") && objs.size() > 1) {
                    log.error("No unique {} called {}", simpleName, name);
                    throw new RuntimeException("No unique "+simpleName+" available");
                } else {
                    obj = objs.get(0);
                }
            }
            id = obj.getId().getValue();
        } else if (discriminator.equals("id")) {
            id = Long.valueOf(name);
        } else {
            log.error("Unknown discriminator {}", discriminator);
            throw new RuntimeException("Unknown discriminator "+discriminator);
        }

        if (subTarget != null) {
            log.info("Super-target loaded: {}:{}", simpleName, id);
            IObject sub = subTarget.load(client, ic);
            // FIXME: need general method
            List<List<omero.RType>> rv = query.projection(
                    "select pdl.id from ProjectDatasetLink pdl where pdl.parent.id = :pid " +
                    "and pdl.child.id = :did",
                    new ParametersI().add("pid", rlong(id)).add("did", sub.getId()));
            if (rv.size() == 0) {
                ProjectDatasetLink pdl = new ProjectDatasetLinkI();
                pdl.setParent(new ProjectI(id, false));
                pdl.setChild(new DatasetI(subTarget.getObjectId(), false));
                update.saveObject(pdl);
                log.info("Linked targets");
            }
            return sub;
        }
        return query.get(type.getSimpleName(), id);
    }
}
