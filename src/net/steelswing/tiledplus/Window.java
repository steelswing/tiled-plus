/*
 * Ну вы же понимаете, что код здесь только мой?
 * Well, you do understand that the code here is only mine?
 */

package net.steelswing.tiledplus;

import java.nio.IntBuffer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.steelswing.engine.api.Timer;
import net.steelswing.engine.api.util.interfaces.Initable;
import net.steelswing.engine.api.util.interfaces.Renderable;
import net.steelswing.flopslop.render.OpenGLCaps;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Callback;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * File: Window.java
 * Created on 2026 Feb 10, 02:49:11
 *
 * @author LWJGL2
 */
@Getter
@Setter
@Accessors(chain = true)
public abstract class Window implements Renderable, Initable {

    private static final Logger LOG = Logger.getLogger(Window.class.getName());

    protected long windowHandle;
    protected static int width = 1280, height = 720;
    protected String title = "TiledPlus";

    protected Timer timer = new Timer(20);
    protected volatile boolean running = true;

    public void create() {
        System.setProperty("joml.format", "false"); // joml fix
        Thread.currentThread().setName("GLFW/MAIN");
        GLFWErrorCallback.createPrint().set();
        if (!GLFW.glfwInit()) {
            throw new RuntimeException("Could not init GLFW");
        }

        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 0); // glPush, glPop(deprecated)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_ANY_PROFILE);

        /**
         * Указывает, должен ли контекст OpenGL быть совместимым с прямой передачей, т.е.
         * таким, в котором удаляются все функции, не рекомендованные в запрашиваемой версии OpenGL.
         * Это должно использоваться только в том случае, если запрашиваемая версия OpenGL 3.0 или выше.
         * Если запрашивается OpenGL ES, эта подсказка игнорируется.
         * <p>
         * glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
         */
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, true ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        windowHandle = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Could not create GLFW window");
        }
        setCallback(GLFW::glfwSetWindowSizeCallback, this::updateSize);

        centerWindow(windowHandle);
        GLFW.glfwMakeContextCurrent(windowHandle);
        GL.createCapabilities();
        OpenGLCaps.initCapabilities();

        GLFW.glfwSwapInterval(true ? 1 : 0);
        GLFW.glfwShowWindow(windowHandle); // показываем окно

        init();
        runLoop();
    }

    public abstract void fixedUpdate();

    protected void runLoop() {
        double lastTime = 0.0; // Время начала формировQания последнего кадра
        while (running) {
            try {
                GLFW.glfwPollEvents();
                if (loop()) {
                    running = false;
                    System.out.println("break");
                    break;
                }

                if (GLFW.glfwWindowShouldClose(windowHandle)) {
                    running = false;
                }

                Thread.yield();
            } catch (Throwable e) {
                e.printStackTrace();
                LOG.log(Level.SEVERE, "GameLoop error", e);
            }
        }

        destruct();
    }

    public boolean loop() {
        return loop(true);
    }

    public boolean loop(boolean pollEvents) {
        try {

            timer.updateTimer();
            for (int i = 0; i < timer.elapsedTicks; i++) {
                fixedUpdate();
            }
            render();
            if (windowHandle == -1) {
                return true;
            }
            GLFW.glfwSwapBuffers(windowHandle);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    protected void updateSize(long window, int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static void centerWindow(long windowHandle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);

            long currentMonitor = GLFW.glfwGetWindowMonitor(windowHandle);
            if (currentMonitor == MemoryUtil.NULL) {
                currentMonitor = GLFW.glfwGetPrimaryMonitor();
            }

            GLFWVidMode videoMode = GLFW.glfwGetVideoMode(currentMonitor);
            if (videoMode == null) {
                throw new RuntimeException("Could not get monitor video mode");
            }

            GLFW.glfwGetWindowSize(windowHandle, width, height);
            GLFW.glfwSetWindowPos(
                    windowHandle,
                    (videoMode.width() - width.get()) / 2,
                    (videoMode.height() - height.get()) / 2
            );
        }
    }

    /**
     * Устанавливает callback GLFW и освобождает старый callback, если он существует.
     *
     * @param setter The function to use for setting the new callback
     * @param newValue The new callback
     * @param <T> The type of the new callback
     * @param <C> The type of the old callback
     */
    public <T, C extends Callback> void setCallback(Function<T, C> setter, T newValue) {
        C oldValue = setter.apply(newValue);
        if (oldValue != null) {
            oldValue.free();
        }
    }

    /**
     * Устанавливает callback GLFW и освобождает старый callback, если он существует.
     *
     * @param setter The function to use for setting the new callback
     * @param newValue The new callback
     * @param <T> The type of the new callback
     * @param <C> The type of the old callback
     */
    public <T, C extends Callback> void setCallback(BiFunction<Long, T, C> setter, T newValue) {
        C oldValue = setter.apply(windowHandle, newValue);
        if (oldValue != null) {
            oldValue.free();
        }
    }

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }
}
