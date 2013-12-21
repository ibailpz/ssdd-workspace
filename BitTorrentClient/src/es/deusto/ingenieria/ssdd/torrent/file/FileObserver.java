package es.deusto.ingenieria.ssdd.torrent.file;

public interface FileObserver {
	
	public void downloaded(int size);
	
	public void finishing();
	
	public void finished();
	
	public void restart();

}
