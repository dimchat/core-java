package chat.dim.core;

public interface ICompletionHandler {

    public void onSuccess();

    public void onFailed(Error error);
}
