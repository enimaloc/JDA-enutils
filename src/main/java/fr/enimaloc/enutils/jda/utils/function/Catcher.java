package fr.enimaloc.enutils.jda.utils.function;

public interface Catcher<T extends Throwable> {
    void run() throws T;
}
