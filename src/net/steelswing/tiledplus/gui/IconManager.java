/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus.gui;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.steelswing.flopslop.render.texture.TextureFilter;
import net.steelswing.flopslop.render.texture.type.ImageData;
import net.steelswing.flopslop.render.texture.type.Texture2D;

/**
 * File: IconManager.java
 * Created on 17 мар. 2024 г., 23:38:51
 *
 * @author LWJGL2
 */
public class IconManager {

    public static final Map<Integer, Texture2D> LOADEDICONS_MAP = new HashMap<>();

    public static void init() {
        IconManager.BRICK.init();
        IconManager.BRICKS.init();
        IconManager.DECOMPILER.init();
        IconManager.DOCUMENT.init();
        IconManager.FOLDER.init();
        IconManager.FORMATS.init();
        IconManager.ICONS.init();
        IconManager.IMAGES.init();
        IconManager.LIGHT.init();
        IconManager.MATERIAL.init();
        IconManager.PAGE.init();
        IconManager.PROJECT.init();
        IconManager.TOOL.init();
        IconManager.TILED.init();
    }

    public static int loadImageIcon(String path) {
        try {
            Texture2D texture = new Texture2D(ImageData.ofAwt(IconManager.class.getResourceAsStream(path)), TextureFilter.LINEAR, false);
            try {
                texture.name = new File(path).getName();
            } catch (Throwable e) {
                texture.name = path;
            }
            texture.loadTexture();
            texture.uploadTextureData();

            LOADEDICONS_MAP.put(texture.getTextureId(), texture);
            return texture.getTextureId();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static final class ICONS {
        // FILE FORMATS 16x16 

        public static final int ICON;
        public static final int ICON_ICON16;
        public static final int ICON16;
        public static final int ICON32;
        public static final int ICON48;

        public static final int TRANSFORM_ICON16;
        public static final int COG_ICON16;
        public static final int TEXT_ICON16;
        public static final int EDIT_ICON16;
        public static final int EDIT_ICON32;

        public static final int RUN_ICON16;
        public static final int STOP_ICON16;

        public static final int ICON_CLOSE_1_16;
        public static final int ICON_CLOSE_2_16;

        public static final int ICON_PACKAGE_16;
        public static final int ICON_WOODEN_BOX_16;

        public static final int ICON_MAGNIFIER_16;
        public static final int ICON_MAGNIFIER_GO_16;

        public static final int ICON_CUP_16;

        public static void init() {

        }

        static {
            // FILE FORMATS 16x16 
            ICON = loadImageIcon(("/assets/used/icons/icon.png"));

            ICON16 = loadImageIcon(("/assets/used/icons/icon_16x16.png"));
            ICON32 = loadImageIcon(("/assets/used/icons/icon_32x32.png"));
            ICON48 = loadImageIcon(("/assets/used/icons/icon_48x48.png"));
            ICON_ICON16 = loadImageIcon(("/assets/used/icon16.png"));

            TRANSFORM_ICON16 = loadImageIcon(("/assets/used/3d.png"));
            TEXT_ICON16 = loadImageIcon(("/assets/used/style.png"));
            COG_ICON16 = loadImageIcon(("/assets/used/cog.png"));
            EDIT_ICON16 = loadImageIcon(("/assets/used/edit16.png"));
            EDIT_ICON32 = loadImageIcon(("/assets/used/edit.png"));

            RUN_ICON16 = loadImageIcon(("/assets/used/play.png"));
            STOP_ICON16 = loadImageIcon(("/assets/used/stop.png"));

            ICON_CLOSE_1_16 = loadImageIcon(("/assets/used/icon_close_1.png"));
            ICON_CLOSE_2_16 = loadImageIcon(("/assets/used/icon_close_2.png"));

            ICON_PACKAGE_16 = loadImageIcon(("/assets/used/old/package.png"));
            ICON_WOODEN_BOX_16 = loadImageIcon(("/assets/used/old/wooden-box.png"));

            ICON_MAGNIFIER_16 = loadImageIcon(("/assets/used/old/magnifier.png"));
            ICON_MAGNIFIER_GO_16 = loadImageIcon(("/assets/used/old/magnifier_go.png"));

            ICON_CUP_16 = loadImageIcon(("/assets/used/cup.png"));
        }
    }

    public static final class FORMATS {
        // FILE FORMATS 16x16 

        public static final int PAGE_FBX_16ICON;
        public static final int PAGE_GLSL_16ICON;
        public static final int PAGE_JAVA_16ICON;
        public static final int PAGE_JAVA_CLASS_16ICON;
        public static final int PAGE_JAVA_CLASSES_16ICON;
        public static final int PAGE_JSON_16ICON;
        public static final int PAGE_OBJ_16ICON;
        public static final int PAGE_PICTURE_16ICON;
        public static final int PAGE_SCENE_16ICON;
        public static final int PAGE_WHITE_16ICON;

        public static List<String> IMAGE_FORMATS = Arrays.asList("png", "jpg", "gif", "bmp", "wbmp");

        public static void init() {

        }

        static {
            // FILE FORMATS 16x16 
            PAGE_FBX_16ICON = loadImageIcon(("/assets/used/page/page_fbx.png"));
            PAGE_GLSL_16ICON = loadImageIcon(("/assets/used/page/page_glsl.png"));
            PAGE_JAVA_16ICON = loadImageIcon(("/assets/used/page/page_java.png"));
            PAGE_JAVA_CLASS_16ICON = loadImageIcon(("/assets/used/page/page_java_class.png"));
            PAGE_JAVA_CLASSES_16ICON = loadImageIcon(("/assets/used/page/page_java_classes.png"));
            PAGE_JSON_16ICON = loadImageIcon(("/assets/used/page/page_json.png"));
            PAGE_OBJ_16ICON = loadImageIcon(("/assets/used/page/page_obj.png"));
            PAGE_PICTURE_16ICON = loadImageIcon(("/assets/used/page/page_picture.png"));
            PAGE_SCENE_16ICON = loadImageIcon(("/assets/used/page/page_scene.png"));
            PAGE_WHITE_16ICON = loadImageIcon(("/assets/used/page/page_white.png"));

        }

        public static boolean isImage(String extention) {
            if (extention != null) {
                extention = extention.toLowerCase();
                return IMAGE_FORMATS.contains(extention);
            }
            return false;
        }

        public static int getByExtention(String extention) {
            if (extention != null) {
                extention = extention.toLowerCase();
                for (String string : IMAGE_FORMATS) {
                    if (extention.endsWith(string)) {
                        return IMAGES.IMAGE_16ICON;
                    }
                }

                if (extention.endsWith("jar") || extention.endsWith("zip")) {
                    return ICONS.ICON_WOODEN_BOX_16;
                }

                if (extention.endsWith("glsl") || extention.endsWith("fs") || extention.endsWith("vs") || extention.endsWith("vert") || extention.endsWith("frag")) {
                    return PAGE_GLSL_16ICON;
                }
                if (extention.endsWith("java")) {
                    return PAGE_JAVA_16ICON;
                }
                if (extention.endsWith("class")) {
                    return PAGE_JAVA_CLASS_16ICON;
                }
                if (extention.endsWith("prefab.json")) {
                    return BRICKS.BRICKS_16ICON;
                }
                if (extention.endsWith("json")) {
                    return PAGE_JSON_16ICON;
                }
                if (extention.endsWith("obj")) {
                    return PAGE_OBJ_16ICON;
                }
                if (extention.endsWith("fbx")) {
                    return PAGE_FBX_16ICON;
                }
                if (extention.endsWith("ssm")) {
                    return BRICK.BRICK_16ICON;
                }

            }
            return PAGE_WHITE_16ICON;
        }
    }

    public static final class BRICK {
        // BRICKS 16x16

        public static final int BRICK_16ICON;
        public static final int BRICK_RGB_16ICON;
        public static final int BRICK_NEW_16ICON;
        public static final int BRICK_DEL_16ICON;
        public static final int BRICK_EDIT_16ICON;

        public static final int BRICK_BLUE_16ICON;
        public static final int BRICK_BLUE_NEW_16ICON;
        public static final int BRICK_BLUE_DEL_16ICON;
        public static final int BRICK_BLUE_EDIT_16ICON;

        // BRICKS 32x32
        public static final int BRICK_32ICON;
        public static final int BRICK_NEW_32ICON;
        public static final int BRICK_DEL_32ICON;
        public static final int BRICK_EDIT_32ICON;

        public static void init() {

        }

        static {
            // BRICKS 16x16
            BRICK_16ICON = loadImageIcon(("/assets/used/brick/brick.png"));
            BRICK_RGB_16ICON = loadImageIcon(("/assets/used/brick/brick_rgb.png"));
            BRICK_NEW_16ICON = loadImageIcon(("/assets/used/brick/brick_new.png"));
            BRICK_DEL_16ICON = loadImageIcon(("/assets/used/brick/brick_delete.png"));
            BRICK_EDIT_16ICON = loadImageIcon(("/assets/used/brick/brick_edit.png"));

            BRICK_BLUE_16ICON = loadImageIcon(("/assets/used/brick/prefab.png"));
            BRICK_BLUE_NEW_16ICON = loadImageIcon(("/assets/used/brick/prefab_new.png"));
            BRICK_BLUE_DEL_16ICON = loadImageIcon(("/assets/used/brick/prefab_delete.png"));
            BRICK_BLUE_EDIT_16ICON = loadImageIcon(("/assets/used/brick/prefab_edit.png"));

            // BRICKS 32x32
            BRICK_32ICON = loadImageIcon(("/assets/used/brick/brick32.png"));
            BRICK_NEW_32ICON = loadImageIcon(("/assets/used/brick/brick32_new.png"));
            BRICK_DEL_32ICON = loadImageIcon(("/assets/used/brick/brick32_delete.png"));
            BRICK_EDIT_32ICON = loadImageIcon(("/assets/used/brick/brick32_edit.png"));
        }
    }

    public static final class BRICKS {
        // BRICKS 16x16

        public static final int BRICKS_16ICON;
        public static final int BRICKS_NEW_16ICON;
        public static final int BRICKS_DEL_16ICON;
        public static final int BRICKS_EDIT_16ICON;

        // BRICKS 32x32
        public static final int BRICKS_32ICON;
        public static final int BRICKS_NEW_32ICON;
        public static final int BRICKS_DEL_32ICON;
        public static final int BRICKS_EDIT_32ICON;

        public static void init() {

        }

        static {
            // BRICKS 16x16
            BRICKS_16ICON = loadImageIcon(("/assets/used/bricks/bricks.png"));
            BRICKS_NEW_16ICON = loadImageIcon(("/assets/used/bricks/bricks_new.png"));
            BRICKS_DEL_16ICON = loadImageIcon(("/assets/used/bricks/bricks_delete.png"));
            BRICKS_EDIT_16ICON = loadImageIcon(("/assets/used/bricks/bricks_edit.png"));

            // BRICKS 32x32
            BRICKS_32ICON = loadImageIcon(("/assets/used/bricks/bricks32.png"));
            BRICKS_NEW_32ICON = loadImageIcon(("/assets/used/bricks/bricks32_new.png"));
            BRICKS_DEL_32ICON = loadImageIcon(("/assets/used/bricks/bricks32_delete.png"));
            BRICKS_EDIT_32ICON = loadImageIcon(("/assets/used/bricks/bricks32_edit.png"));
        }
    }

    public static final class LIGHT {
        // LIGHT 16x16

        public static final int LIGHT_16ICON;
        public static final int LIGHT_NEW_16ICON;
        public static final int LIGHT_DEL_16ICON;

        public static final int LIGHT_DIRECTION_16ICON;
        public static final int LIGHT_DIRECTION_NEW_16ICON;
        public static final int LIGHT_DIRECTION_DEL_16ICON;

        public static final int LIGHT_PROJECTOR_16ICON;
        public static final int LIGHT_PROJECTOR_NEW_16ICON;
        public static final int LIGHT_PROJECTOR_DEL_16ICON;

        // LIGHT 32x32
        public static final int LIGHT_32ICON;
        public static final int LIGHT_NEW_32ICON;
        public static final int LIGHT_DEL_32ICON;

        public static final int LIGHT_DIRECTION_32ICON;
        public static final int LIGHT_DIRECTION_NEW_32ICON;
        public static final int LIGHT_DIRECTION_DEL_32ICON;

        public static final int LIGHT_PROJECTOR_32ICON;
        public static final int LIGHT_PROJECTOR_NEW_32ICON;
        public static final int LIGHT_PROJECTOR_DEL_32ICON;

        public static void init() {

        }

        static {
            // LIGHT 16x16
            LIGHT_16ICON = loadImageIcon(("/assets/used/light/light.png"));
            LIGHT_NEW_16ICON = loadImageIcon(("/assets/used/light/light_new.png"));
            LIGHT_DEL_16ICON = loadImageIcon(("/assets/used/light/light_delete.png"));

            // LIGHT 16x16
            LIGHT_DIRECTION_16ICON = loadImageIcon(("/assets/used/light/light_dir.png"));
            LIGHT_DIRECTION_NEW_16ICON = loadImageIcon(("/assets/used/light/light_dir_new.png"));
            LIGHT_DIRECTION_DEL_16ICON = loadImageIcon(("/assets/used/light/light_dir_delete.png"));

            // LIGHT PROJECTOR 16x16
            LIGHT_PROJECTOR_16ICON = loadImageIcon(("/assets/used/light/light_projector.png"));
            LIGHT_PROJECTOR_NEW_16ICON = loadImageIcon(("/assets/used/light/light_projector_new.png"));
            LIGHT_PROJECTOR_DEL_16ICON = loadImageIcon(("/assets/used/light/light_projector_delete.png"));

            // LIGHT 32x32
            LIGHT_32ICON = loadImageIcon(("/assets/used/light/light32.png"));
            LIGHT_NEW_32ICON = loadImageIcon(("/assets/used/light/light32_new.png"));
            LIGHT_DEL_32ICON = loadImageIcon(("/assets/used/light/light32_delete.png"));

            LIGHT_DIRECTION_32ICON = loadImageIcon(("/assets/used/light/light_dir.png"));
            LIGHT_DIRECTION_NEW_32ICON = loadImageIcon(("/assets/used/light/light_dir_new.png"));
            LIGHT_DIRECTION_DEL_32ICON = loadImageIcon(("/assets/used/light/light_dir_delete.png"));

            LIGHT_PROJECTOR_32ICON = loadImageIcon(("/assets/used/light/light_projector32.png"));
            LIGHT_PROJECTOR_NEW_32ICON = loadImageIcon(("/assets/used/light/light_projector32_new.png"));
            LIGHT_PROJECTOR_DEL_32ICON = loadImageIcon(("/assets/used/light/light_projector32_delete.png"));
        }
    }

    public static final class TOOL {

        // TOOL 16x16
        public static final int NEW_16ICON;
        public static final int REDO_16ICON;
        public static final int UNDO_16ICON;
        public static final int REFRESH_16ICON;
        public static final int WARN_16ICON;

        public static final int INFO_16ICON;
        public static final int QUEST_16ICON;
        public static final int SAVE_16ICON;
        public static final int EXIT_16ICON;
        public static final int CROSS_16ICON;

        public static final int TREE_EXPAND_16ICON;
        public static final int TREE_COLLAPSE_16ICON;

        // TOOL 32x32

        public static final int NEW_32ICON;
        public static final int WARN_32ICON;
        public static final int QUEST_32ICON;
        public static final int CROSS_32ICON;


        public static final int SELECT_32ICON;
        public static final int TRANSLATE_32ICON;
        public static final int SCALE_32ICON;
        public static final int ROTATE_32ICON;

        public static final int SELECT_SELECTED_32ICON;
        public static final int TRANSLATE_SELECTED_32ICON;
        public static final int SCALE_SELECTED_32ICON;
        public static final int ROTATE_SELECTED_32ICON;

        public static final int RUN_ICON32;
        public static final int STOP_ICON32;


        public static final int TOOL_SELECTION32ICON;
        public static final int TOOL_BRUSH32ICON;
        public static final int TOOL_BRUSH_SURFACE_32ICON;
        public static final int TOOL_RULER_32ICON;

        public static final int PIPETTE_TOOL_32ICON;
        public static final int SMOOTH_TOOL_32ICON;
        public static final int CAMERA_TOOL_32ICON;
        public static final int PREFAB_ADD_32ICON;

        public static void init() {

        }

        static {
            PIPETTE_TOOL_32ICON = loadImageIcon(("/assets/used/tool/pipette_tool.png"));
            SMOOTH_TOOL_32ICON = loadImageIcon(("/assets/used/tool/smooth_tool.png"));
            CAMERA_TOOL_32ICON = loadImageIcon(("/assets/used/tool/camera_tool.png"));
            PREFAB_ADD_32ICON = loadImageIcon(("/assets/used/brick/prefab32_add.png"));

            TOOL_SELECTION32ICON = loadImageIcon(("/assets/used/tool/tool_selection.png"));
            TOOL_BRUSH32ICON = loadImageIcon(("/assets/used/tool/tool_brush.png"));
            TOOL_BRUSH_SURFACE_32ICON = loadImageIcon(("/assets/used/tool/tool_surface_brush.png"));
            TOOL_RULER_32ICON = loadImageIcon(("/assets/used/tool/tool_ruler.png"));

            //
            TREE_EXPAND_16ICON = loadImageIcon(("/assets/used/tool/tree_down.png"));
            TREE_COLLAPSE_16ICON = loadImageIcon(("/assets/used/tool/tree_up.png"));

            // LIGHT 16x16
            NEW_16ICON = loadImageIcon(("/assets/used/tool/new.png"));
            REDO_16ICON = loadImageIcon(("/assets/used/tool/redo.png"));
            UNDO_16ICON = loadImageIcon(("/assets/used/tool/undo.png"));
            REFRESH_16ICON = loadImageIcon(("/assets/used/tool/refresh.png"));
            WARN_16ICON = loadImageIcon(("/assets/used/tool/warn.png"));
            SAVE_16ICON = loadImageIcon(("/assets/used/tool/save.png"));
            EXIT_16ICON = loadImageIcon(("/assets/used/tool/exit.png"));
            CROSS_16ICON = loadImageIcon(("/assets/used/tool/cross.png"));

            INFO_16ICON = loadImageIcon(("/assets/used/tool/info.png"));
            QUEST_16ICON = loadImageIcon(("/assets/used/tool/quest.png"));

            // LIGHT 32x32
            NEW_32ICON = loadImageIcon(("/assets/used/tool/new32.png"));
            WARN_32ICON = loadImageIcon(("/assets/used/tool/warn32.png"));
            QUEST_32ICON = loadImageIcon(("/assets/used/tool/quest32.png"));
            CROSS_32ICON = loadImageIcon(("/assets/used/tool/cross32.png"));

            // gizmo
            SELECT_32ICON = loadImageIcon(("/assets/used/tool/button_select.png"));
            TRANSLATE_32ICON = loadImageIcon(("/assets/used/tool/button_translate.png"));
            SCALE_32ICON = loadImageIcon(("/assets/used/tool/button_scale.png"));
            ROTATE_32ICON = loadImageIcon(("/assets/used/tool/button_rotate.png"));

            SELECT_SELECTED_32ICON = loadImageIcon(("/assets/used/tool/button_select_selected.png"));
            TRANSLATE_SELECTED_32ICON = loadImageIcon(("/assets/used/tool/button_translate_selected.png"));
            SCALE_SELECTED_32ICON = loadImageIcon(("/assets/used/tool/button_scale_selected.png"));
            ROTATE_SELECTED_32ICON = loadImageIcon(("/assets/used/tool/button_rotate_selected.png"));

            RUN_ICON32 = loadImageIcon(("/assets/used/tool/play32.png"));
            STOP_ICON32 = loadImageIcon(("/assets/used/tool/button_Stop.png"));
        }
    }

    public static final class FOLDER {

        // FOLDER 16x16
        public static final int FOLDER_CLOSE_16ICON;
        public static final int FOLDER_NEW_16ICON;
        public static final int FOLDER_OPEN_16ICON;

        public static final int JAVA_PROJECT_16ICON;

        public static void init() {

        }

        static {
            // FOLDER 16x16
            FOLDER_CLOSE_16ICON = loadImageIcon(("/assets/used/folder/folder_close.png"));
            FOLDER_NEW_16ICON = loadImageIcon(("/assets/used/folder/folder_new.png"));
            FOLDER_OPEN_16ICON = loadImageIcon(("/assets/used/folder/folder_open.png"));

            JAVA_PROJECT_16ICON = loadImageIcon(("/assets/used/folder/j2seProject.png"));
        }
    }

    public static final class DOCUMENT {

        // FOLDER 16x16
        public static final int DOCUMENT_BLUEPRINT_64ICON;
        public static final int DOCUMENT_EMITTER_64ICON;
        public static final int DOCUMENT_IMAGE_64ICON;
        public static final int DOCUMENT_SOUND_64ICON;
        public static final int DOCUMENT_VIDEO_64ICON;

        public static final int DOCUMENT_PNG_64ICON;
        public static final int DOCUMENT_DDS_64ICON;
        public static final int DOCUMENT_FBX_64ICON;
        public static final int DOCUMENT_OBJ_64ICON;
        public static final int DOCUMENT_UNKNOWN_64ICON;

        public static final int DOCUMENT_FOLDER_CLOSE_64ICON;
        public static final int DOCUMENT_FOLDER_OPEN_64ICON;

        public static final int DOCUMENT_MATERIAL_64ICON;
        public static final int DOCUMENT_SCENE_64ICON;
        public static final int DOCUMENT_ZIP_64ICON;

        static {
            DOCUMENT_BLUEPRINT_64ICON = loadImageIcon(("/assets/used/document/document_blueprint.png"));
            DOCUMENT_EMITTER_64ICON = loadImageIcon(("/assets/used/document/document_emitter.png"));
            DOCUMENT_IMAGE_64ICON = loadImageIcon(("/assets/used/document/document_img.png"));
            DOCUMENT_SOUND_64ICON = loadImageIcon(("/assets/used/document/document_sound.png"));
            DOCUMENT_VIDEO_64ICON = loadImageIcon(("/assets/used/document/document_video.png"));

            DOCUMENT_PNG_64ICON = loadImageIcon(("/assets/used/document/document_png.png"));
            DOCUMENT_DDS_64ICON = loadImageIcon(("/assets/used/document/document_dds.png"));
            DOCUMENT_FBX_64ICON = loadImageIcon(("/assets/used/document/document_fbx.png"));
            DOCUMENT_OBJ_64ICON = loadImageIcon(("/assets/used/document/document_obj.png"));
            DOCUMENT_UNKNOWN_64ICON = loadImageIcon(("/assets/used/document/document_unknown.png"));

            DOCUMENT_FOLDER_CLOSE_64ICON = loadImageIcon(("/assets/used/document/folder_close.png"));
            DOCUMENT_FOLDER_OPEN_64ICON = loadImageIcon(("/assets/used/document/folder_open.png"));

            DOCUMENT_MATERIAL_64ICON = loadImageIcon(("/assets/used/document/document_mat.png"));
            DOCUMENT_SCENE_64ICON = loadImageIcon(("/assets/used/document/document_scene.png"));
            DOCUMENT_ZIP_64ICON = loadImageIcon(("/assets/used/document/folder_zip.png"));
        }

        public static void init() {
        }

        public static int getByExtention(String extention) {
            if (extention != null) {
                extention = extention.toLowerCase();

                if (extention.endsWith("png")) {
                    return DOCUMENT_PNG_64ICON;
                }
                if (extention.endsWith("dds")) {
                    return DOCUMENT_DDS_64ICON;
                }
                if (extention.endsWith("mp4") || extention.endsWith("avi")) {
                    return DOCUMENT_VIDEO_64ICON;
                }
                if (extention.endsWith("bmp") || extention.endsWith("gif") || extention.endsWith("bmp") || extention.endsWith("wbmp") || extention.endsWith("jpg")) {
                    return DOCUMENT_IMAGE_64ICON;
                }

                if (extention.endsWith("fbx")) {
                    return DOCUMENT_FBX_64ICON;
                }
                if (extention.endsWith("obj")) {
                    return DOCUMENT_OBJ_64ICON;
                }
                if (extention.endsWith("ogg") || extention.endsWith("wav") || extention.endsWith("mp3")) {
                    return DOCUMENT_SOUND_64ICON;
                }

                if (extention.endsWith("jar") || extention.endsWith("zip")) {
                    return DOCUMENT_ZIP_64ICON;
                }

                if (extention.endsWith("glsl") || extention.endsWith("fs") || extention.endsWith("vs") || extention.endsWith("vert") || extention.endsWith("frag")) {
                    return DOCUMENT_MATERIAL_64ICON;
                }

                if (extention.endsWith("ssm")) {
                    return DOCUMENT_BLUEPRINT_64ICON;
                }

                if (extention.endsWith("prefab.json")) {
                    return DOCUMENT_SCENE_64ICON;
                }
            }
            return DOCUMENT_UNKNOWN_64ICON;
        }
    }

    public static final class PROJECT {

        // FOLDER 16x16
        public static final int PROJECT_NEW_16ICON;
        public static final int PROJECT_LOAD_16ICON;

        public static void init() {

        }

        static {
            PROJECT_NEW_16ICON = loadImageIcon(("/assets/used/project/application_new.png"));
            PROJECT_LOAD_16ICON = loadImageIcon(("/assets/used/project/application_get.png"));
        }
    }

    public static final class MATERIAL {

        public static final int MATERIAL_16ICON;
        public static final int MATERIAL_NEW_16ICON;

        public static final int MATERIAL_32ICON;
        public static final int MATERIAL_NEW_32ICON;
        public static final int MATERIAL_DELETE_32ICON;

        public static void init() {

        }

        static {
            MATERIAL_16ICON = loadImageIcon(("/assets/used/material/color_wheel.png"));
            MATERIAL_NEW_16ICON = loadImageIcon(("/assets/used/material/color_wheel_new.png"));

            MATERIAL_32ICON = loadImageIcon(("/assets/used/material/color_wheel32.png"));
            MATERIAL_NEW_32ICON = loadImageIcon(("/assets/used/material/color_wheel_new32.png"));
            MATERIAL_DELETE_32ICON = loadImageIcon(("/assets/used/material/color_wheel_delete32.png"));
        }
    }

    public static final class IMAGES {

        public static final int IMAGE_16ICON;
        public static final int IMAGE_LINK_16ICON;

        public static void init() {

        }

        static {
            IMAGE_16ICON = loadImageIcon(("/assets/used/image/image.png"));
            IMAGE_LINK_16ICON = loadImageIcon(("/assets/used/image/image_link.png"));

        }
    }

    public static final class PAGE {

        public static final int PAGE_SCRIPT_16ICON;

        public static void init() {

        }

        static {
            PAGE_SCRIPT_16ICON = loadImageIcon(("/assets/used/page/script.png"));
        }
    }

    public static final class DECOMPILER {

        public static final int APP_FERNFLOWER_16ICON;
        public static final int APP_CFR_16ICON;
        public static final int APP_PROCYON_16ICON;

        public static void init() {

        }

        static {
            APP_FERNFLOWER_16ICON = loadImageIcon(("/assets/used/decompiler/fernflower.png"));
            APP_CFR_16ICON = loadImageIcon(("/assets/used/decompiler/cfr.png"));
            APP_PROCYON_16ICON = loadImageIcon(("/assets/used/decompiler/procyon.png"));
        }
    }
    public static final class TILED {

        public static final int LAYER_OBJECT;
        public static final int LAYER_TILES;
        public static final int PAGE_ADD;
        public static final int PAGE_DELETE;
        public static final int PUT;
        public static final int ERASE;
        public static final int FILL;

        public static void init() {

        }

        static {
            LAYER_OBJECT = loadImageIcon(("/assets/used/tiled/layer-object.png"));
            LAYER_TILES = loadImageIcon(("/assets/used/tiled/layer-tile.png"));
            
            PAGE_ADD = loadImageIcon(("/assets/used/tiled/page_add.png"));
            PAGE_DELETE = loadImageIcon(("/assets/used/tiled/page_delete.png"));
            
            PUT = loadImageIcon(("/assets/used/tiled/stock-tool-clone.png"));
            ERASE = loadImageIcon(("/assets/used/tiled/stock-tool-eraser.png"));
            FILL = loadImageIcon(("/assets/used/tiled/stock-tool-bucket-fill.png"));
//            PUT = loadImageIcon(("/assets/used/tiled/page_delete.png"));
        }
    }
}
