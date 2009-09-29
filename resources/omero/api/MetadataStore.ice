/*
 *   $Id$
 *
 *   Copyright 2008 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef OMERO_METADATASTORE_ICE
#define OMERO_METADATASTORE_ICE

#include <Ice/BuiltinSequences.ice>
#include <omero/API.ice>
#include <omero/Scripts.ice>

module omero {

    module constants {

        const string METADATASTORE = "omero.api.MetadataStore";

    };

    /**
     * Types used during import.
     **/
    module metadatastore {

        /**
         * Container-class used by the import mechanism. Passed to [omero::api::MetadataStore]
         **/
        class IObjectContainer {
            string LSID;
            omero::api::StringIntMap indexes;
            omero::model::IObject sourceObject;
        };

        sequence<IObjectContainer> IObjectContainerArray;

    };

    module api {

        /**
         * Server-side interface for import.
         **/
        ["ami","amd"] interface MetadataStore extends StatefulServiceInterface
            {
                void createRoot() throws ServerError;
                void updateObjects(omero::metadatastore::IObjectContainerArray objects) throws ServerError;
                void updateReferences(omero::api::StringStringArrayMap references) throws ServerError;
                PixelsList saveToDB() throws ServerError;
                void populateMinMax(DoubleArrayArrayArray imageChannelGlobalMinMax) throws ServerError;
                omero::grid::InteractiveProcessor* postProcess() throws ServerError;
            };
    };

};
#endif
