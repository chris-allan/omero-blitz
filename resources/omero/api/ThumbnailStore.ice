/*
 *   $Id$
 *
 *   Copyright 2010 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef OMERO_API_THUMBNAILSTORE_ICE
#define OMERO_API_THUMBNAILSTORE_ICE

#include <omero/ModelF.ice>
#include <omero/ServicesF.ice>
#include <omero/Collections.ice>

module omero {

    module api {

        /**
         * See <a href="http://hudson.openmicroscopy.org.uk/job/OMERO/javadoc/ome/api/ThumbnailStore.html">ThumbnailStore.html</a>
         **/
        ["ami", "amd"] interface ThumbnailStore extends StatefulServiceInterface
            {
                bool setPixelsId(long pixelsId) throws ServerError;
                void setRenderingDefId(long renderingDefId) throws ServerError;
                Ice::ByteSeq getThumbnail(omero::RInt sizeX, omero::RInt sizeY) throws ServerError;
                omero::sys::IdByteMap getThumbnailSet(omero::RInt sizeX, omero::RInt sizeY, omero::sys::LongList pixelsIds) throws ServerError;
                omero::sys::IdByteMap getThumbnailByLongestSideSet(omero::RInt size, omero::sys::LongList pixelsIds) throws ServerError;
                Ice::ByteSeq getThumbnailByLongestSide(omero::RInt size) throws ServerError;
                Ice::ByteSeq getThumbnailByLongestSideDirect(omero::RInt size) throws ServerError;
                Ice::ByteSeq getThumbnailDirect(omero::RInt sizeX, omero::RInt sizeY) throws ServerError;
                Ice::ByteSeq getThumbnailForSectionDirect(int theZ, int theT, omero::RInt sizeX, omero::RInt sizeY) throws ServerError;
                Ice::ByteSeq getThumbnailForSectionByLongestSideDirect(int theZ, int theT, omero::RInt size) throws ServerError;
                void createThumbnails() throws ServerError;
                void createThumbnail(omero::RInt sizeX, omero::RInt sizeY) throws ServerError;
                void createThumbnailsByLongestSideSet(omero::RInt size, omero::sys::LongList pixelsIds) throws ServerError;
                bool thumbnailExists(omero::RInt sizeX, omero::RInt sizeY) throws ServerError;
                void resetDefaults() throws ServerError;
            };
    };
};

#endif
